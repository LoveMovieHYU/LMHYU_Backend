package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Converter.BiorhythmConverter;
import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Biorhythm.Dto.AiResponseDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDTO;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class RecommendService {

    /** AI 추천 서버 응답 대기 타임아웃 (무한 대기 방지) */
    private static final Duration AI_API_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient; // RestTemplate 대신 주입
    private final UserService userService;
    private final RecommendQueryService recommendQueryService;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendService(WebClient webClient, UserService userService,
                            RecommendQueryService recommendQueryService, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.userService = userService;
        this.recommendQueryService = recommendQueryService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 첫 로그인 유저인 경우 이메일, 생년월일 저장 후, 영화 추천 받음.
     * 바이오리듬 수치 Redis 에 저장까지함.
     * 사용자 정보 저장(쓰기 트랜잭션)은 {@link UserService#updateUserInfo} 에서 먼저 커밋되고,
     * AI 호출·Redis 저장은 트랜잭션 밖에서 수행되어 DB 커넥션을 외부 I/O 동안 점유하지 않는다.
     * */
    public List<MovieAiRecommendationDTO> savedDetailUserInfoAndRecommend(int userId, UpdateUserRequestDTO requestDTO){

        // 쓰기 트랜잭션: 여기서 커밋 완료 (별도 빈 프록시 호출)
        userService.updateUserInfo(userId, requestDTO);

        // 트랜잭션 밖에서 AI 호출·Redis 저장 수행
        return getbiorhythmBasedRecommendation(userId);
    }

    /**
     * 바이오리듬 커스텀 기반 영화 추천
     * */
    public List<MovieAiRecommendationDTO> getCustomRecommend(Double p, Double e, Double i, int userId){
        User user = recommendQueryService.getUser(userId);
        int age = getAge(user);
        String gender = getGender(user);

        AiResponseDTO[] aiResponseArray = callAiApi(userId, p, e, i, age, gender);

        if (aiResponseArray == null || aiResponseArray.length == 0) {
            log.info("AI 서버가 빈 추천 목록을 반환했습니다. userId={}", userId);
            return List.of();
        }

        return recommendQueryService.getMovieAiRecommendationDTOs(aiResponseArray);
    }

    /**
     * 바이오리듬 기반 추천 (AI 연동 -> DB 조회 -> Redis 저장)
     * 1. Redis 캐시 확인 -> 있으면 반환
     * 2. 없으면 AI 요청 -> DB 조회 -> Redis 저장 (영화와 감정)-> 반환
     * 외부 AI 호출은 트랜잭션 밖에서 수행하고, DB 조회는 {@link RecommendQueryService} 의
     * 읽기 전용 트랜잭션으로 범위를 좁힌다.
     */
    public List<MovieAiRecommendationDTO> getbiorhythmBasedRecommendation(int userId) {

        String cacheKey = getMovieListCacheKey(userId);

        try {
            List<MovieAiRecommendationDTO> cachedData = readMovieListCache(cacheKey);

            if (cachedData != null) {
                log.info("Cache Hit! Returning data from Redis for user: {}", userId);
                return cachedData;
            }
        } catch (Exception e) {
            log.error("Redis get failed", e);
        }

        User user = recommendQueryService.getUser(userId);
        BiorhythmScore biorhythmScore = calculateScores(user.getBirthday());

        saveBioRedis(getBioCacheKey(userId), biorhythmScore); // 바이오리듬 분석 수치 Redis 저장
        log.info("Saved Bio info in Chach ");

        int age = getAge(user);
        String gender = getGender(user);
        double p = biorhythmScore.getPhysical();
        double e = biorhythmScore.getEmotional();
        double i = biorhythmScore.getIntellectual();

        // AI 서버 호출 (트랜잭션 밖)
        log.info("Requesting AI Recommendation for user: {}, p={}, e={}, i={}", userId, p, e, i);
        AiResponseDTO[] aiResponseArray = callAiApi(userId, p, e, i, age, gender);

        if (aiResponseArray == null || aiResponseArray.length == 0) {
            log.info("AI 서버가 빈 추천 목록을 반환했습니다. userId={}", userId);
            return List.of();
        }

        // Movie ID 추출 (읽기 전용 트랜잭션)
        List<MovieAiRecommendationDTO> responseDTO = recommendQueryService.getMovieAiRecommendationDTOs(aiResponseArray);

        // Redis 저장
        try {
            redisTemplate.opsForValue().set(cacheKey, responseDTO, 1, TimeUnit.DAYS);
            log.info("Saved recommendations to Redis. Size: {}", responseDTO.size());
        } catch (Exception ex) {
            log.error("Redis save failed", ex);
        }

        return responseDTO;
    }

    /**
     * AI 영화 리스트 조회 API 호출.
     * 외부 연동 실패(HTTP 오류/연결 실패/응답 타임아웃)는 {@link ErrorCode#EXTERNAL_API_ERROR} 로 변환해 던진다.
     * 그 외 예외(프로그래밍 오류 등)는 감추지 않고 그대로 전파한다.
     * */
    private AiResponseDTO[] callAiApi(int userId, double p, double e, double i, int age, String gender) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/recommend/movie/{userId}/{age}/{gender}/{p}/{e}/{i}")
                            .build(userId, age, gender, p, e, i))
                    .retrieve()
                    .bodyToMono(AiResponseDTO[].class)
                    .timeout(AI_API_TIMEOUT) // 명시적 응답 타임아웃으로 무한 대기 방지
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("AI 서버 응답 오류: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "추천 서버 연동에 실패했습니다.");
        } catch (WebClientException ex) {
            // 연결 실패 등 요청 자체가 실패한 경우
            log.error("AI 서버 연결 실패", ex);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "추천 서버 연동에 실패했습니다.");
        } catch (RuntimeException ex) {
            // reactor 타임아웃은 TimeoutException 을 감싼 RuntimeException 으로 전달된다.
            if (isTimeout(ex)) {
                log.error("AI 서버 응답 타임아웃 (>{}초)", AI_API_TIMEOUT.getSeconds(), ex);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "추천 서버 연동에 실패했습니다.");
            }
            // 그 외 예외는 삼키지 않고 그대로 전파 (프로그래밍 오류 은폐 방지)
            throw ex;
        }
    }

    /**
     * 예외 원인 체인에 타임아웃이 포함되어 있는지 확인한다.
     * */
    private boolean isTimeout(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof TimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * 유저 성별 문자열 반환 (없으면 예외)
     * */
    private String getGender(User user) {
        if (user.getGender() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "유저의 성별 정보가 없습니다.");
        }
        return user.getGender().toString();
    }

    /**
     * 바이오리듬 분석 결과 DTO 변환
     * */
    public BiorhythmAnalysisDTO analyzeBiorhythm(int userId) {
        User user = recommendQueryService.getUser(userId);

        // 점수 계산
        BiorhythmScore score = calculateScores(user.getBirthday());
        String birthday = user.getBirthday().toString();
        String message = getStatusMessage(score);

        saveBioRedis(getBioCacheKey(userId), score); // 바이오리듬 분석 수치 Redis 저장

        return BiorhythmConverter.toAnalysisDTO(score, message, birthday);
    }

    /**
     * 바이오리듬 Redis 저장
     * */
    public void saveBioRedis(String key, BiorhythmScore score){
        // Redis 저장
        try {
            redisTemplate.opsForValue().set(key, score, 1, TimeUnit.DAYS);
            log.info("Saved Bio info in Chach ");
        } catch (Exception ex) {
            log.error("Redis save failed", ex);
        }
    }

    /**
     * 나이 계산
     * */
    private static int getAge(User user) {
        LocalDate birthday = user.getBirthday();
        LocalDate now = LocalDate.now();
        return (int) ChronoUnit.YEARS.between(birthday, now);
    }

    /**
     * 바이오리듬 Reids 조회
     * */
    public BiorhythmScore getBioCache(int userId) {
        String key = getBioCacheKey(userId);

        BiorhythmScore biorhythmScore = (BiorhythmScore) redisTemplate.opsForValue().get(key);

        if (biorhythmScore != null) {
            log.info("Cache Hit! Returning data from Redis for user: {}", userId);
        } else {
            log.info("Cache Miss for user: {}", userId);
        }
        return biorhythmScore;

    }

    /**
     * 조회 및 없으면 계산 후 저장 ( 로그 보낼 때 사용 )
     * */
    public BiorhythmScore getOrCalculateBiorhythm(int userId) {
        BiorhythmScore cachedScore = getBioCache(userId);
        if (cachedScore != null) {
            return cachedScore;
        }

        User user = recommendQueryService.getUser(userId);
        BiorhythmScore calculatedScore = calculateScores(user.getBirthday());

        saveBioRedis(getBioCacheKey(userId), calculatedScore);
        log.info("Bio Saved in Cache");

        return calculatedScore;
    }

    /**
     * 영화 추천 리스트 캐시를 안전하게 읽는다.
     * (RedisConfig 가 GenericJackson2Json 으로 타입 정보를 보존하므로 실제 타입은 유지되지만,
     *  역직렬화 결과가 예상과 다를 경우 캐시 미스로 처리한다.)
     */
    private List<MovieAiRecommendationDTO> readMovieListCache(String cacheKey) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }
        if (!(cached instanceof List<?> list)) {
            log.warn("Unexpected cache type for key={}, type={}", cacheKey, cached.getClass());
            return null;
        }
        if (list.stream().anyMatch(e -> !(e instanceof MovieAiRecommendationDTO))) {
            log.warn("Unexpected cache element type for key={}", cacheKey);
            return null;
        }
        List<MovieAiRecommendationDTO> result = new java.util.ArrayList<>();
        for (Object e : list) {
            result.add((MovieAiRecommendationDTO) e);
        }
        return result;
    }

    /**
     * Redis Key 생성 (영화 리스트)
     * */
    private String getMovieListCacheKey(int userId) {
        return "recommend:biorhythm:" + userId + ":" + LocalDate.now();
    }

    /**
     * Redis Key 생성 (바이오리듬 리스트)
     * */
    private String getBioCacheKey(int userId) {
        return "biorhythm:" + userId + ":" + LocalDate.now();
    }




    /**
     * 바이오리듬 수치 계산
     * */
    private BiorhythmScore calculateScores(LocalDate birthDay) {
        long daysLived = ChronoUnit.DAYS.between(birthDay, LocalDate.now());

        // 공식: sin(2 * pi * t / 주기) * 100
        double p = (Math.sin((2 * Math.PI * daysLived) / 23) * 100);
        double e = (Math.sin((2 * Math.PI * daysLived) / 28) * 100);
        double i = (Math.sin((2 * Math.PI * daysLived) / 33) * 100);

        return new BiorhythmScore(p, e, i);
    }
    /**
     * 바이오리듬 수치 기반 + 연구 근거 반영 추천 멘트
     */
    private String getStatusMessage(BiorhythmScore s) {
        int p = (int) s.getPhysical();
        int e = (int) s.getEmotional();
        int i = (int) s.getIntellectual();

        int absP = Math.abs(p);
        int absE = Math.abs(e);
        int absI = Math.abs(i);

        int maxAbs = Math.max(absP, Math.max(absE, absI));

        if (maxAbs < 70) {
            return "오늘은 전반적으로 바이오리듬 밸런스가 좋은 상태예요. 특정 장르에 치우치기보다는, 다양한 감정을 경험할 수 있는 평점 높은 웰메이드 명작을 감상해 보는 것을 추천드려요.";
        }

        if (maxAbs == absP) {
            return p > 0
                    ? "오늘은 신체적 에너지와 각성(Arousal) 수준이 높은 상태예요. 넘치는 에너지를 발산하며 몰입할 수 있는 액션이나 SF 등 역동적인 영화를 추천드려요."
                    : "오늘은 신체적 에너지가 다소 낮아 휴식이 필요한 상태예요. 에너지를 많이 쓰지 않고도 앉은 자리에서 강렬하게 몰입할 수 있는 긴장감 넘치는 스릴러나 서사 중심의 영화를 추천드려요.";

        } else if (maxAbs == absE) {
            return e > 0
                    ? "오늘은 감수성이 풍부하고 감정에 깊게 동화될 수 있는 상태예요. 풍부한 감정선을 따라가는 드라마나 로맨스 장르를 통해 감정적 몰입(Pleasure)을 극대화해 보세요."
                    : "오늘은 감정적 에너지가 다소 낮아 기분 전환이 필요한 상태예요. 연구에 따르면 코미디나 카타르시스를 주는 영화가 즐거움을 높이는 데 효과적이니, 자극적인 작품을 추천드려요.";

        } else {
            return i > 0
                    ? "오늘은 지적 호기심(Openness)과 사고력이 매우 높은 상태예요. 단순한 오락을 넘어, 탄탄한 세계관의 SF, 다큐멘터리, 혹은 깊은 메시지를 던지는 영화를 추천드려요."
                    : "오늘은 복잡한 생각보다는 직관적인 즐거움이 필요한 상태예요. 뇌를 비우고 편안하게 시각적, 감각적으로 즐길 수 있는 킬링타임용 팝콘 무비를 추천드려요.";
        }
    }

}

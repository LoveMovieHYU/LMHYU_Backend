package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Converter.BiorhythmConverter;
import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Biorhythm.Converter.RecommendConverter;
import Recommend.Movie.Biorhythm.Dto.AiResponseDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MovieGenre;
import Recommend.Movie.Tmdb.Repository.MovieGenreRepository;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import Recommend.Movie.User.Repository.UserRepository;
import Recommend.Movie.User.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class RecommendService {

    private final WebClient webClient; // RestTemplate 대신 주입
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MovieGenreRepository movieGenreRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendService(WebClient webClient, MovieRepository movieRepository,
                            UserRepository userRepository, UserService userService, MovieGenreRepository movieGenreRepository, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.movieGenreRepository = movieGenreRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 첫 로그인 유저인 경우 이메일, 생년월일 저장 후, 영화 추천 받음.
     * 바이오리듬 수치 Redis 에 저장까지함.
     * */
    @Transactional
    public List<MovieAiRecommendationDTO> savedDetailUserInfoAndRecommend(int userId, UpdateUserRequestDTO requestDTO){

        userService.updateUserInfo(userId, requestDTO);

        List<MovieAiRecommendationDTO> responseDTO = getbiorhythmBasedRecommendation(userId);
        return responseDTO;

    }
    /**
     * 바이오리듬 커스텀 기반 영화 추천
     * */
    public List<MovieAiRecommendationDTO> getCustomRecommend(Double p, Double e, Double i, int userId){
        AiResponseDTO[] aiResponseArray = null;
        User user = getUser(userId);
        int age = getAge(user);

        String gender;
        if (user.getGender() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "유저의 성별 정보가 없습니다.");
        } else {
            gender = user.getGender().toString();
        }
        try {
            aiResponseArray = callAiApi(userId, p, e, i,age, gender);
        } catch (WebClientResponseException ex) {
            log.error("AI Server Error: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return List.of();
        } catch (Exception ex) {
            log.error("AI Server Connection Failed", ex);
            return List.of();
        }

        if (aiResponseArray == null || aiResponseArray.length == 0) {
            log.info("AI Server returned empty list.");
            return List.of();
        }

        List<MovieAiRecommendationDTO> responseDTO = getMovieAiRecommendationDTOs(aiResponseArray);

        return responseDTO;
    }

    /**
     * 바이오리듬 기반 추천 (AI 연동 -> DB 조회 -> Redis 저장)
     * 1. Redis 캐시 확인 -> 있으면 반환
     * 2. 없으면 AI 요청 -> DB 조회 -> Redis 저장 (영화와 감정)-> 반환
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

        User user = getUser(userId);
        BiorhythmScore biorhythmScore = calculateScores(user.getBirthday());

        saveBioRedis(getBioCacheKey(userId),biorhythmScore); // 바이오리듬 분석 수치 Redis 저장
        log.info("Saved Bio info in Chach ");

        int age = getAge(user);
        String gender = "W";
        if (user.getGender() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "유저의 성별 정보가 없습니다.");
        } else {
            gender = user.getGender().toString();
        }
        double p = biorhythmScore.getPhysical();
        double e = biorhythmScore.getEmotional();
        double i = biorhythmScore.getIntellectual();

        // AI 서버 호출
        log.info("Requesting AI Recommendation for user: {}, p={}, e={}, i={}", userId, p, e, i);
        AiResponseDTO[] aiResponseArray = null;

        try {
            aiResponseArray = callAiApi(userId, p, e, i,age, gender);
        } catch (WebClientResponseException ex) {
            log.error("AI Server Error: Status={}, Body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            // 필요시 예외 처리 (빈 리스트 반환 or 커스텀 예외 던지기)
            return List.of();
        } catch (Exception ex) {
            log.error("AI Server Connection Failed", ex);
            return List.of();
        }

        if (aiResponseArray == null || aiResponseArray.length == 0) {
            log.info("AI Server returned empty list.");
            return List.of();
        }

        // Movie ID 추출
        List<MovieAiRecommendationDTO> responseDTO = getMovieAiRecommendationDTOs(aiResponseArray);

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
     * TmdbID 를 활용하여 영화 DB 접근 후 DTO 변환
     * */
    private List<MovieAiRecommendationDTO> getMovieAiRecommendationDTOs(AiResponseDTO[] aiResponseArray) {
        List<Long> tmdbIds = Arrays.stream(aiResponseArray)
                .map(AiResponseDTO::getMovieId)
                .collect(Collectors.toList());

        List<Movie> movies = movieRepository.findAllByTmdbIdIn(tmdbIds);

        List<Integer> movieIds = movies.stream()
                .map(Movie::getId)
                .collect(Collectors.toList());

        // 추출한 영화 PK 목록으로 장르 매핑 정보를 한번에 조회 ( N+1 문제 방지 )
        List<MovieGenre> allMovieGenres = movieGenreRepository.findByMovie_IdIn(movieIds);

        Map<Integer, List<MovieGenre>> genreMap = allMovieGenres.stream()
                .collect(Collectors.groupingBy(mg -> mg.getMovie().getId())); // 장르를 영화 ID 기준으로 그룹화

        Map<Long, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity())); // tmdbId를 key 로 가지는 Movie Map 생성

        List<MovieAiRecommendationDTO> responseDTO = tmdbIds.stream()
                .map(movieMap::get)
                .filter(Objects::nonNull)
                .map(movie -> {
                    List<MovieGenre> genres = genreMap.getOrDefault(movie.getId(), Collections.emptyList());
                    return RecommendConverter.fromEntity(movie, genres);
                })
                .sorted(Comparator.comparing(MovieAiRecommendationDTO::getPopularity, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return responseDTO;
    }

    /**
     * AI 영화 리스트 조회 API 호출
     * */
    private AiResponseDTO[] callAiApi(int userId, double p, double e, double i, int age, String gender) {
        AiResponseDTO[] aiResponseArray;
        aiResponseArray = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/recommend/movie/{userId}/{age}/{gender}/{p}/{e}/{i}")
                        .build(userId,age,gender, p, e, i))
                .retrieve()
                .bodyToMono(AiResponseDTO[].class)
                .block();
        return aiResponseArray;

    }

    /**
     * 바이오리듬 분석 결과 DTO 변환
     * */
    public BiorhythmAnalysisDTO analyzeBiorhythm(int userId) {
        User user = getUser(userId);

        // 점수 계산
        BiorhythmScore score = calculateScores(user.getBirthday());
        String birthday = user.getBirthday().toString();
        String message = getStatusMessage(score);

        saveBioRedis(getBioCacheKey(userId),score); // 바이오리듬 분석 수치 Redis 저장

        return BiorhythmConverter.toAnalysisDTO(score, message,birthday);
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
        int age = (int) ChronoUnit.YEARS.between(birthday, now);
        return age;
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

        User user = getUser(userId);
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
        List<MovieAiRecommendationDTO> result = new ArrayList<>();
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

        BiorhythmScore biorhythmScore = new BiorhythmScore(p, e, i);
        return biorhythmScore;
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

    private User getUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));
        if (user.getBirthday() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "생년월일 정보가 필요합니다. 마이페이지에서 설정해주세요.");
        }
        return user;
    }


}
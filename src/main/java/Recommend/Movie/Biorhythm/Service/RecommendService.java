package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Converter.BiorhythmConverter;
import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Biorhythm.Converter.RecommendConverter;
import Recommend.Movie.Biorhythm.Dto.AiResponseDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDto;
import Recommend.Movie.Tmdb.Domain.Movie;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final RedisTemplate<String, Object> redisTemplate;

    public RecommendService(WebClient webClient, MovieRepository movieRepository,
                            UserRepository userRepository, UserService userService, RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 첫 로그인 유저인 경우 이메일, 생년월일 저장 후, 영화 추천 받음.
     * 바이오리듬 수치 Redis 에 저장까지함.
     * */
    @Transactional
    public List<MovieAiRecommendationDto> savedDetailUserInfoAndRecommend(int userId, UpdateUserRequestDTO requestDTO){

        userService.updateUserInfo(userId, requestDTO);

        List<MovieAiRecommendationDto> responseDTO = getbiorhythmBasedRecommendation(userId);
        return responseDTO;

    }
    /**
     * 바이오리듬 커스텀 기반 영화 추천
     * */
    public List<MovieAiRecommendationDto> getCustomRecommend(Double p, Double e, Double i, int userId){
        AiResponseDTO[] aiResponseArray = null;

        try {
            aiResponseArray = callAiApi(userId, p, e, i);
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

        List<MovieAiRecommendationDto> responseDTO = getMovieAiRecommendationDtos(aiResponseArray);

        return responseDTO;
    }

    /**
     * 바이오리듬 기반 추천 (AI 연동 -> DB 조회 -> Redis 저장)
     * 1. Redis 캐시 확인 -> 있으면 반환
     * 2. 없으면 AI 요청 -> DB 조회 -> Redis 저장 (영화와 감정)-> 반환
     */
    public List<MovieAiRecommendationDto> getbiorhythmBasedRecommendation(int userId) {
        
        String cacheKey = getMovieListCacheKey(userId);

        try {
            List<MovieAiRecommendationDto> cachedData =
                    (List<MovieAiRecommendationDto>) redisTemplate.opsForValue().get(cacheKey);

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


        double p = biorhythmScore.getPhysical();
        double e = biorhythmScore.getEmotional();
        double i = biorhythmScore.getIntellectual();

        // AI 서버 호출
        log.info("Requesting AI Recommendation for user: {}, p={}, e={}, i={}", userId, p, e, i);
        AiResponseDTO[] aiResponseArray = null;

        try {
            aiResponseArray = callAiApi(userId, p, e, i);
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
        List<MovieAiRecommendationDto> responseDTO = getMovieAiRecommendationDtos(aiResponseArray);

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
    private List<MovieAiRecommendationDto> getMovieAiRecommendationDtos(AiResponseDTO[] aiResponseArray) {
        List<Long> tmdbIds = Arrays.stream(aiResponseArray)
                .map(AiResponseDTO::getMovieId)
                .collect(Collectors.toList());

        // DB 조회
        List<Movie> movies = movieRepository.findAllByTmdbIdIn(tmdbIds);

        Map<Long, Movie> movieMap = movies.stream()
                .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));

        List<MovieAiRecommendationDto> responseDTO = tmdbIds.stream()
                .map(movieMap::get)
                .filter(Objects::nonNull)
                .map(RecommendConverter::fromEntity)
                .collect(Collectors.toList());
        return responseDTO;
    }

    /**
     * AI 영화 리스트 조회 API 호출
     * */
    private AiResponseDTO[] callAiApi(int userId, double p, double e, double i) {
        AiResponseDTO[] aiResponseArray;
        aiResponseArray = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/recommend/movie/{userId}/{p}/{e}/{i}")
                        .build(userId, p, e, i))
                .retrieve()
                .bodyToMono(AiResponseDTO[].class)
                .block();
        return aiResponseArray;

//        // 1. 일단 String으로 받아서 로그 찍어보기
//        String rawResponse = webClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/api/recommend/movie/{userId}/{p}/{e}/{i}")
//                        .build(userId, p, e, i))
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//
//        log.info("AI Server Raw Response: {}", rawResponse);
//
//        try {
//            ObjectMapper mapper = new ObjectMapper();
//            // JSON이 배열 '[' 로 시작하는지 확인
//            if (rawResponse != null && rawResponse.trim().startsWith("[")) {
//                return mapper.readValue(rawResponse, AiResponseDTO[].class);
//            } else {
//                log.error("AI Server returned non-array response: {}", rawResponse);
//                return new AiResponseDTO[0];
//            }
//        } catch (JsonProcessingException ex) {
//            log.error("JSON Parsing Error", ex);
//            return new AiResponseDTO[0];
//        }

    }

    /**
     * 바이오리듬 분석 결과 DTO 변환
     * */
    public BiorhythmAnalysisDTO analyzeBiorhythm(int userId) {
        User user = getUser(userId);

        // 점수 계산
        BiorhythmScore score = calculateScores(user.getBirthday());

        String message = getStatusMessage(score);

        saveBioRedis(getBioCacheKey(userId),score); // 바이오리듬 분석 수치 Redis 저장
        
        return BiorhythmConverter.toAnalysisDTO(score, message);
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
     * 바이오리듬 Reids 조회
     * */
    public BiorhythmScore getBioCache(int userId) {
        String key = getBioCacheKey(userId);

        BiorhythmScore biorhythmScore = (BiorhythmScore) redisTemplate.opsForValue().get(key);

        if (biorhythmScore == null) {
            log.info("Cache Hit! Returning data from Redis for user: {}", userId);
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
     * 바이오리듬 수치에 따른 멘트 선정 로직
     * */
    private String getStatusMessage(BiorhythmScore s) {
        // 가장 특징적인(높거나 낮은) 점수를 기반으로 멘트 결정
        if (s.getPhysical() > 70) return "오늘은 에너지가 넘치는 날이네요! 활동적인 영화가 끌리실 거예요.";
        if (s.getEmotional() > 70) return "감수성이 풍부해지는 오늘, 마음을 울리는 영화 한 편 어때요?";
        if (s.getIntellectual() > 70) return "두뇌 회전이 빠른 날입니다. 몰입감 넘치는 스토리에 도전해보세요!";
        if (s.getPhysical() < -70) return "몸이 조금 무거운 날이네요. 편안한 영화로 휴식을 취해보세요.";
        if (s.getEmotional() < -70) return "기분이 조금 가라앉을 땐, 실컷 웃거나 짜릿한 영화로 기분 전환!";

        return "전반적으로 밸런스가 좋은 날입니다. 평점이 높은 명작 영화들을 추천해요!";
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
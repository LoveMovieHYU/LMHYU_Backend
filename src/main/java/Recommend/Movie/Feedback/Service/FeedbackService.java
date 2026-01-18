package Recommend.Movie.Feedback.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Feedback.Converter.FeedBackConverter;
import Recommend.Movie.Feedback.Domain.FeedbackEvent;
import Recommend.Movie.Feedback.Dto.TodayEmotionRequestDTO;
import Recommend.Movie.Feedback.Dto.TodayEmotionResponseDTO;
import Recommend.Movie.Feedback.Repository.FeedbackEventRepository;
import Recommend.Movie.Feedback.Dto.MovieReactionRequestDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FeedbackService {

    private final FeedbackEventRepository feedbackEventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public FeedbackService(FeedbackEventRepository feedbackEventRepository, RedisTemplate<String, String> redisTemplate, UserRepository userRepository, MovieRepository movieRepository) {
        this.feedbackEventRepository = feedbackEventRepository;
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * 추천된 영화 반응 저장
     * */
    @Transactional
    public String saveMovieReaction(int movieId, MovieReactionRequestDTO requestDTO,
                                    String userId){
        User user = userRepository.findByUserId(Integer.parseInt(userId));
        if(user == null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "유저를 찾을 수 없습니다.");
        }
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "해당 영화가 존재하지 않습니다."));


        Optional<FeedbackEvent> existingFeedback = feedbackEventRepository.findByUserAndMovie(user, movie);

        if(existingFeedback.isPresent()){
            FeedbackEvent feedback = existingFeedback.get();
            feedback.updateReaction(requestDTO.getReactionType(), requestDTO.getEmotionTag());
            return "반응이 수정됐습니다.";
        }else{
            FeedbackEvent feedbackEvent = FeedBackConverter.toEntity(requestDTO, user, movie);
            user.addFeedback(feedbackEvent);
            feedbackEventRepository.save(feedbackEvent);
            return "성공했습니다.";
        }
    }

    /**
     * 오늘의 감정 Redis에 저장
     * */
    public String saveEmotionRedis(TodayEmotionRequestDTO requestDTO, int userId){
        String key = buildKey(userId);
        String value = requestDTO.getEmotionTag().name();

        long expiredSeconds = expiredCalc();
        redisTemplate.opsForValue().set(key, value, expiredSeconds, TimeUnit.SECONDS);
        return "오늘의 감정이 저장되었습니다.";
    }

    /**
     * 오늘의 감정 조회
     * */
    public TodayEmotionResponseDTO searchTodayEmotion(int userId){
        String key = buildKey(userId);
        String todyEmotion = redisTemplate.opsForValue().get(key);
        if(todyEmotion == null){
            return new TodayEmotionResponseDTO("오늘 감정 선택 하지 않았습니다.",false,
                    null);
        }
        return new TodayEmotionResponseDTO("오늘의 감정입니다.", true, todyEmotion);
    }

    /**
     * 현재 시간과 자정 사이의 차이(초 단위) 계산
     * */
    private static long expiredCalc() {
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        ZonedDateTime now = ZonedDateTime.now(kstZone);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(kstZone);

        return Duration.between(now, midnight).getSeconds();
    }

    /**
     * 키 생성
     * */
    private static String buildKey(int userId){
        return "today_emotion:" + userId;
    }
}

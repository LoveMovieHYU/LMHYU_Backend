package Recommend.Movie.Emotion.Service;

import Recommend.Movie.Feedback.Dto.TodayEmotionRequestDTO;
import Recommend.Movie.Feedback.Dto.TodayEmotionResponseDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class EmotionService {

    private final RedisTemplate<String, String> redisTemplate;

    public EmotionService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
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

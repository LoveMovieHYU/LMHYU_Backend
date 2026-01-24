package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Converter.BiorhythmConverter;
import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Biorhythm.Dto.CheckResponseDTO;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Movies.Dto.MoviePreviewResponseDTO;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecommendService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate; // Redis 의존성

    public RecommendService(UserRepository userRepository, RedisTemplate<String, Object> redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    // --- 캐시 확인 및 조회 로직 ---
    public CheckResponseDTO checkAndGetCache(int userId) {
        String cacheKey = getCacheKey(userId);

        // Redis에서 데이터 꺼내기
        List<MoviePreviewResponseDTO> cachedMovies =
                (List<MoviePreviewResponseDTO>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedMovies != null) {
            // 캐시 있음: 영화 리스트와 함께 true 반환
            return CheckResponseDTO.builder()
                    .isCached(true)
                    .movieList(cachedMovies)
                    .build();
        } else {
            // 캐시 없음: false 반환 (리스트는 null)
            return CheckResponseDTO.builder()
                    .isCached(false)
                    .movieList(null)
                    .build();
        }
    }


    public BiorhythmAnalysisDTO analyzeBiorhythm(int userId) {
        User user = getUser(userId);

        // 점수 계산
        BiorhythmScore score = calculateScores(user.getBirthday());

        String message = getStatusMessage(score);

        return BiorhythmConverter.toAnalysisDTO(score, message);
    }


    // Redis Key 생성: "recommend:biorhythm:{userId}:{yyyy-MM-dd}"
    private String getCacheKey(int userId) {
        return "recommend:biorhythm:" + userId + ":" + LocalDate.now();
    }

    // 유저 조회 헬퍼
    private User getUser(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));
        if (user.getBirthday() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "생년월일 정보가 필요합니다. 마이페이지에서 설정해주세요.");
        }
        return user;
    }

    // 바이오리듬 점수 계산
    private BiorhythmScore calculateScores(LocalDate birthDay) {
        long daysLived = ChronoUnit.DAYS.between(birthDay, LocalDate.now());

        // 공식: sin(2 * pi * t / 주기) * 100
        int p = (int) (Math.sin((2 * Math.PI * daysLived) / 23) * 100);
        int e = (int) (Math.sin((2 * Math.PI * daysLived) / 28) * 100);
        int i = (int) (Math.sin((2 * Math.PI * daysLived) / 33) * 100);

        return new BiorhythmScore(p, e, i);
    }

    // 점수에 따른 멘트 선정 로직
    private String getStatusMessage(BiorhythmScore s) {
        // 가장 특징적인(높거나 낮은) 점수를 기반으로 멘트 결정
        if (s.getPhysical() > 70) return "오늘은 에너지가 넘치는 날이네요! 활동적인 영화가 끌리실 거예요.";
        if (s.getEmotional() > 70) return "감수성이 풍부해지는 오늘, 마음을 울리는 영화 한 편 어때요?";
        if (s.getIntellectual() > 70) return "두뇌 회전이 빠른 날입니다. 몰입감 넘치는 스토리에 도전해보세요!";
        if (s.getPhysical() < -70) return "몸이 조금 무거운 날이네요. 편안한 영화로 휴식을 취해보세요.";
        if (s.getEmotional() < -70) return "기분이 조금 가라앉을 땐, 실컷 웃거나 짜릿한 영화로 기분 전환!";

        return "전반적으로 밸런스가 좋은 날입니다. 평점이 높은 명작 영화들을 추천해요!";
    }


}
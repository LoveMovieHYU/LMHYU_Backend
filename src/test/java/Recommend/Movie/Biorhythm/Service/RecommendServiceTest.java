package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDTO;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Domain.Gender;
import Recommend.Movie.User.Domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @InjectMocks
    private RecommendService recommendService;

    @Mock
    private RecommendQueryService recommendQueryService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private User testUser;

    @BeforeEach
    void setUp(){
        testUser = User.builder()
                .userId(1)
                .birthday(LocalDate.of(2002, 8, 24))
                .gender(Gender.valueOf("M"))
                .build();
    }

    /**
     * Redis Cache hit 테스트
     * */
    @Test
    @DisplayName("Redis에 영화 추천 캐시가 있으면 AI 서버를 호출하지 않고 캐시 데이터를 반환한다 (Cache Hit)")
    void getRecommendation_CacheHit() {
        // given
        int userId = 1;
        List<MovieAiRecommendationDTO> mockCachedList = List.of(
                MovieAiRecommendationDTO.builder().title("캐시된 영화").build()
        );

        // RedisTemplate의 opsForValue()가 모의 객체를 반환하도록 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(mockCachedList);

        // when
        List<MovieAiRecommendationDTO> result = recommendService.getbiorhythmBasedRecommendation(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.get(0).getTitle()).isEqualTo("캐시된 영화");

        // 캐시 적중 시 DB 조회(유저 조회)가 일어나지 않아야 한다
        verify(recommendQueryService, never()).getUser(userId);
    }

    @Test
    @DisplayName("유저 조회에서 생년월일이 없어 BAD_REQUEST 예외가 발생하면 그대로 전파한다")
    void analyzeBiorhythm_WithoutBirthday_ThrowsException() {
        // given
        int userId = 1;
        when(recommendQueryService.getUser(userId))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "생년월일 정보가 필요합니다. 마이페이지에서 설정해주세요."));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendService.analyzeBiorhythm(userId));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("생년월일 정보가 필요합니다");
    }

    @Test
    @DisplayName("유저 조회에서 존재하지 않는 유저면 USER_NOT_FOUND 예외를 그대로 전파한다")
    void analyzeBiorhythm_UserNotFound_ThrowsException() {
        // given
        int userId = 999;
        when(recommendQueryService.getUser(userId))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND, "유저가 없습니다."));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendService.analyzeBiorhythm(userId));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("커스텀 추천 시 성별 정보가 없으면 BAD_REQUEST 예외가 발생")
    void getCustomRecommend_WithoutGender_ThrowsException() {
        // given
        int userId = 1;
        User noGenderUser = User.builder()
                .userId(userId)
                .birthday(LocalDate.of(2002, 8, 24))
                .build();
        when(recommendQueryService.getUser(userId)).thenReturn(noGenderUser);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendService.getCustomRecommend(0.5, 0.1, 0.2, userId));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("정상적인 유저 정보를 통해 바이오리듬을 분석하고 DTO를 반환한다")
    void analyzeBiorhythm_Success() {
        // given
        int userId = 1;
        when(recommendQueryService.getUser(userId)).thenReturn(testUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        BiorhythmAnalysisDTO result = recommendService.analyzeBiorhythm(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getBirthDay()).isEqualTo("2002-08-24");
        assertThat(result.getStatusMessage().isBlank()); // 리팩토링한 멘트가 잘 들어갔는지 확인

        // Redis에 바이오리듬 수치가 잘 저장(set) 되었는지 검증
        verify(valueOperations, times(1)).set(anyString(), any(), anyLong(), any());
    }

}

package Recommend.Movie.Biorhythm.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendQueryServiceTest {

    @InjectMocks
    private RecommendQueryService recommendQueryService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("유저 조회 시 존재하지 않는 유저면 USER_NOT_FOUND 예외가 발생")
    void getUser_UserNotFound_ThrowsException() {
        // given
        int userId = 999;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendQueryService.getUser(userId));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("유저 조회 시 생년월일이 없으면 BAD_REQUEST 예외가 발생")
    void getUser_WithoutBirthday_ThrowsException() {
        // given
        int userId = 1;
        User noBirthdayUser = User.builder().userId(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(noBirthdayUser));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> recommendQueryService.getUser(userId));

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(exception.getMessage()).contains("생년월일 정보가 필요합니다");
    }

    @Test
    @DisplayName("정상 유저면 유저 엔티티를 반환한다")
    void getUser_Success() {
        // given
        int userId = 1;
        User user = User.builder()
                .userId(userId)
                .birthday(LocalDate.of(2002, 8, 24))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        User result = recommendQueryService.getUser(userId);

        // then
        assertThat(result).isSameAs(user);
    }
}

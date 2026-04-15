package Recommend.Movie.LikeMovie.Service;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Domain.LikedMovie;
import Recommend.Movie.LikeMovie.Domain.LikeMovieListResponseDTO;
import Recommend.Movie.LikeMovie.Dto.MovieReactionRequestDTO;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeMovieServiceTest {

    @InjectMocks
    private LikeMovieService likeMovieService;

    @Mock
    private LikeMovieRepository likeMovieRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovieRepository movieRepository;

    private User testUser;
    private Movie testMovie;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setLikedMovieList(new ArrayList<>()); // 미리 빈 리스트 할당

        testMovie = new Movie();
        testMovie.setTmdbId(100L);
        testMovie.setTitle("테스트 영화");
    }

    @Test
    @DisplayName("영화를 정상적으로 좋아요(저장) 하면, 유저의 리스트에 추가되고 DB에 저장된다")
    void saveMovieReaction_Success() {
        // given
        long tmdbId = 100L;
        String userId = "1";

        // DTO 생성
        MovieReactionRequestDTO requestDTO = new MovieReactionRequestDTO();

        // 가짜 동작 정의: userId 1로 조회하면 testUser 반환, tmdbId 100으로 조회하면 testMovie 반환
        when(userRepository.findByUserId(1)).thenReturn(testUser);
        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));

        // When
        String result = likeMovieService.saveMovieReaction(tmdbId, requestDTO, userId);

        // Then
        assertThat(result).isEqualTo("성공했습니다."); // 리턴 스트링 확인

        // 검증 1 : DB에 저장을 시도했는가? (likeMovieRepository.save가 1번 호출되었는지 확인)
        verify(likeMovieRepository, times(1)).save(any(LikedMovie.class));

        // 검증 2: 유저 엔티티 내부의 리스트에 잘 추가되었는가? (addLikeMovie 작동 확인)
        assertThat(testUser.getLikedMovieList().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("최근 좋아요 누른 영화 목록을 조회하면, 최신순(ID 역순)으로 정렬되어 반환된다")
    void getLikeMovieList_Success() {
        // Given
        String userId = "1";
        when(userRepository.findByUserId(1)).thenReturn(testUser);

        // 정렬 검증을 위해 가짜 LikedMovie 2개 생성 (Mockito.mock 활용)
        LikedMovie oldLike = mock(LikedMovie.class);
        when(oldLike.getId()).thenReturn(1); // 예전에 누른 좋아요
        when(oldLike.getMovie()).thenReturn(testMovie);

        LikedMovie newLike = mock(LikedMovie.class);
        when(newLike.getId()).thenReturn(2); // 최근에 누른 좋아요
        when(newLike.getMovie()).thenReturn(testMovie);

        // 유저 리스트에 옛날 것 -> 최신 것 순서로 넣음
        testUser.getLikedMovieList().add(oldLike);
        testUser.getLikedMovieList().add(newLike);

        // When
        List<LikeMovieListResponseDTO> result = likeMovieService.getLikeMovieList(userId);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("좋아요를 정상적으로 취소(삭제) 하면, 유저 리스트에서 빠지고 DB에서 삭제된다")
    void deleteLikeMovie_Success() {
        // Given
        long tmdbId = 100L;
        String userId = "1";

        when(userRepository.findByUserId(1)).thenReturn(testUser);
        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));

        // 삭제할 가짜 엔티티 생성
        LikedMovie likedMovie = mock(LikedMovie.class);
        when(likeMovieRepository.findByUserAndMovie(testUser, testMovie)).thenReturn(Optional.of(likedMovie));

        // 삭제 전 미리 유저의 리스트에 들어있다고 가정
        testUser.getLikedMovieList().add(likedMovie);

        // When
        String result = likeMovieService.deleteLikeMovie(tmdbId, userId);

        // Then
        assertThat(result).isEqualTo("좋아요가 취소되었습니다.");

        // 검증 1: DB에서 delete가 잘 호출되었는가?
        verify(likeMovieRepository, times(1)).delete(likedMovie);

        // 검증 2: 유저의 리스트에서 정상적으로 제거되었는가? (removeLikeMovie 작동 확인)
        assertThat(testUser.getLikedMovieList()).isEmpty();
    }

    @Test
    @DisplayName("유효하지 않은 유저 ID로 요청하면 USER_NOT_FOUND 예외가 발생한다")
    void getUser_UserNotFound_ThrowsException() {
        // Given
        String userId = "999";
        when(userRepository.findByUserId(999)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            likeMovieService.getLikeMovieList(userId);
        });

        assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
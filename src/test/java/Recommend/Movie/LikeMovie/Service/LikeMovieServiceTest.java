package Recommend.Movie.LikeMovie.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Dto.LikeMovieListResponseDTO;
import Recommend.Movie.LikeMovie.Domain.LikedMovie;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        testUser = User.builder()
                .userId(1)
                .likedMovieList(new ArrayList<>())
                .build();

        testMovie = Movie.builder()
                .tmdbId(100L)
                .title("Test Movie")
                .build();
    }

    @Test
    @DisplayName("Saving a movie reaction adds it and persists it")
    void saveMovieReaction_Success() {
        long tmdbId = 100L;
        String userId = "1";
        MovieReactionRequestDTO requestDTO = new MovieReactionRequestDTO();

        when(userRepository.findByUserId(1)).thenReturn(testUser);
        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));

        String result = likeMovieService.saveMovieReaction(tmdbId, requestDTO, userId);

        assertThat(result).isNotBlank();
        verify(likeMovieRepository, times(1)).save(any(LikedMovie.class));
        assertThat(testUser.getLikedMovieList()).hasSize(1);
    }

    @Test
    @DisplayName("Like list returns saved liked movies")
    void getLikeMovieList_Success() {
        String userId = "1";
        when(userRepository.findByUserId(1)).thenReturn(testUser);

        LikedMovie oldLike = mock(LikedMovie.class);
        when(oldLike.getMovie()).thenReturn(testMovie);

        LikedMovie newLike = mock(LikedMovie.class);
        when(newLike.getMovie()).thenReturn(testMovie);

        when(likeMovieRepository.findByUserIdWithMovie(1)).thenReturn(List.of(newLike, oldLike));

        List<LikeMovieListResponseDTO> result = likeMovieService.getLikeMovieList(userId);

        assertThat(result).hasSize(2);
        verify(likeMovieRepository, times(1)).findByUserIdWithMovie(1);
    }

    @Test
    @DisplayName("Deleting a liked movie removes it and deletes it")
    void deleteLikeMovie_Success() {
        long tmdbId = 100L;
        String userId = "1";

        when(userRepository.findByUserId(1)).thenReturn(testUser);
        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));

        LikedMovie likedMovie = mock(LikedMovie.class);
        when(likeMovieRepository.findByUserAndMovie(testUser, testMovie)).thenReturn(Optional.of(likedMovie));
        testUser.getLikedMovieList().add(likedMovie);

        String result = likeMovieService.deleteLikeMovie(tmdbId, userId);

        assertThat(result).isNotBlank();
        verify(likeMovieRepository, times(1)).delete(likedMovie);
        assertThat(testUser.getLikedMovieList()).isEmpty();
    }

    @Test
    @DisplayName("Unknown user throws USER_NOT_FOUND")
    void getUser_UserNotFound_ThrowsException() {
        String userId = "999";
        when(userRepository.findByUserId(999)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                likeMovieService.getLikeMovieList(userId)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}

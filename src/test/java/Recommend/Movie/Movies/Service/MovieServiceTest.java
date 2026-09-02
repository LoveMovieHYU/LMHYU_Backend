package Recommend.Movie.Movies.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Tmdb.Repository.MoviePeopleRepository;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @InjectMocks
    private MovieService movieService;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private LikeMovieRepository likeMovieRepository;

    @Mock
    private MoviePeopleRepository moviePeopleRepository;

    private Movie testMovie;

    @BeforeEach
    void setUp() {
        testMovie = Movie.builder()
                .id(104)
                .tmdbId(671L)
                .title("Harry Potter and the Sorcerer's Stone")
                .build();
    }

    @Test
    @DisplayName("Search keyword is normalized for native full-text search")
    void searchMovies_Success() {
        String keyword = "Harry Potter";
        int page = 1;
        String expectedSearchKeyword = "+Harry +Potter";
        String expectedNoSpaceKeyword = "HarryPotter";
        Page<Movie> moviePage = new PageImpl<>(List.of(testMovie));

        when(movieRepository.searchByKeywordNative(
                eq(expectedSearchKeyword),
                eq(expectedNoSpaceKeyword),
                any(Pageable.class)
        )).thenReturn(moviePage);

        List<MovieSearchResponseDTO> result = movieService.searchMovies(keyword, page);

        assertThat(result).hasSize(1);
        verify(movieRepository, times(1)).searchByKeywordNative(
                eq(expectedSearchKeyword),
                eq(expectedNoSpaceKeyword),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Movie detail includes liked state for logged-in user")
    void getMovieDetail_Success_WithLoginUser() {
        long tmdbId = 100L;
        int userId = 17;

        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));
        when(moviePeopleRepository.findByMovie_Id(testMovie.getId())).thenReturn(List.of());
        when(likeMovieRepository.existsByUser_UserIdAndMovie_TmdbId(userId, tmdbId)).thenReturn(true);

        MovieDetailResponse result = movieService.getMovieDetail(tmdbId, userId);

        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isTrue();
        verify(likeMovieRepository, times(1)).existsByUser_UserIdAndMovie_TmdbId(userId, tmdbId);
    }

    @Test
    @DisplayName("Guest movie detail does not query liked state")
    void getMovieDetail_Success_WithGuestUser() {
        long tmdbId = 100L;
        int userId = 0;

        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.of(testMovie));
        when(moviePeopleRepository.findByMovie_Id(testMovie.getId())).thenReturn(List.of());

        MovieDetailResponse result = movieService.getMovieDetail(tmdbId, userId);

        assertThat(result).isNotNull();
        verify(likeMovieRepository, never()).existsByUser_UserIdAndMovie_TmdbId(anyInt(), anyLong());
    }

    @Test
    @DisplayName("Unknown movie id throws BusinessException")
    void getMovieDetail_MovieNotFound_ThrowsException() {
        long tmdbId = 999L;
        int userId = 1;

        when(movieRepository.findByTmdbId(tmdbId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.getMovieDetail(tmdbId, userId)
        );

        assertThat(exception.getCode()).isEqualTo(ErrorCode.MOVIE_NOT_FOUND);
    }
}

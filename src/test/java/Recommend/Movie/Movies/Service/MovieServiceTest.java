package Recommend.Movie.Movies.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.LikeMovie.Repository.LikeMovieRepository;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @InjectMocks
    private MovieService movieService;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private LikeMovieRepository likeMovieRepository;

    private Movie testMovie;

    @BeforeEach
    void setUp() {
        testMovie = new Movie();
        testMovie.setTmdbId(671L);
        testMovie.setId(104);
        testMovie.setTitle("해리 포터와 마법사의 돌");
    }

    @Test
    @DisplayName("검색어가 전문 검색(FTS) 쿼리 포맷으로 올바르게 파싱되어 DB를 조회")
    void searchMovies_Success() {
        // given
        String keyword = "해리 포터";
        int page = 1;

        // 서비스 내부 로직에 의해 변환될 것으로 기대되는 문자열
        String expectedSearchKeyword = "+해리 +포터";
        String expectedNoSpaceKeyword = "해리포터";

        // Repository가 반환할 가짜 Page 객체 생성
        Page<Movie> moviePage = new PageImpl<>(List.of(testMovie));

        when(movieRepository.searchByKeywordNative(
                eq(expectedSearchKeyword),
                eq(expectedNoSpaceKeyword),
                any(Pageable.class)
        )).thenReturn(moviePage);

        // when
        List<MovieSearchResponseDTO> result = movieService.searchMovies(keyword, page);

        // then
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);

        verify(movieRepository, times(1)).searchByKeywordNative(
                eq(expectedSearchKeyword),
                eq(expectedNoSpaceKeyword),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("로그인한 유저가 조회할 때, 영화 상세 정보와 함께 좋아요한 영화 상태가 반환")
    void getMovieDetail_Success_WithLoginUser() {

        // given
        long tmdbId = 100L;
        int userId = 17;

        when(movieRepository.findByTmdbIdWithPeople(tmdbId)).thenReturn(Optional.of(testMovie));
        // 이 유저는 이 영화를 찜했다고 가정 (true 반환)
        when(likeMovieRepository.existsByUser_UserIdAndMovie_TmdbId(userId, tmdbId)).thenReturn(true);

        // when
        MovieDetailResponse result = movieService.getMovieDetail(tmdbId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.isLiked()).isTrue();

        verify(likeMovieRepository, times(1)).existsByUser_UserIdAndMovie_TmdbId(userId, tmdbId);
    }

    @Test
    @DisplayName("로그인하지 않은 게스트 유저가 조회할 때, 찜(Like) 조회 로직을 타지 않는다")
    void getMovieDetail_Success_WithGuestUser() {
        // given
        long tmdbId = 100L;
        int userId = 0;

        when(movieRepository.findByTmdbIdWithPeople(tmdbId)).thenReturn(Optional.of(testMovie));

        // when
        MovieDetailResponse result = movieService.getMovieDetail(tmdbId, userId);

        // then
        assertThat(result).isNotNull();
        // userId가 0이므로 DB 조회 쿼리 자체가 날아가지 않아야 함을 검증 (성능 최적화 포인트)
        verify(likeMovieRepository, never()).existsByUser_UserIdAndMovie_TmdbId(anyInt(), anyLong());
    }

    @Test
    @DisplayName("DB에 존재하지 않는 영화 ID를 조회하면 BusinessException이 발생한다")
    void getMovieDetail_MovieNotFound_ThrowsException() {
        // given
        long tmdbId = 999L;
        int userId = 1;

        when(movieRepository.findByTmdbIdWithPeople(tmdbId)).thenReturn(Optional.empty());

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            movieService.getMovieDetail(tmdbId, userId);
        });

        assertThat(exception.getMessage()).contains("영화를 찾을 수 없습니다");
    }
}
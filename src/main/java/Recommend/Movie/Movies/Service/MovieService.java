package Recommend.Movie.Movies.Service;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Movies.Converter.MovieConverter;
import Recommend.Movie.Movies.Dto.HomeResponseDTO;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Movies.Repository.MovieSpecification;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Tmdb.Domain.Genre;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.GenreRepository;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;


    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * 영화 검색 ( 영화 이름, 배우(감독) 이름 )
     * */
    public List<MovieSearchResponseDTO> searchMovies(String keyword, int page){

        int pageNum = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNum, 10, Sort.by(Sort.Direction.DESC, "releaseDate"));

        Specification<Movie> spec = MovieSpecification.searchByKeyword(keyword);

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        return moviePage.getContent().stream()
                .map(MovieConverter::toSearchDTO)
                .collect(Collectors.toList());

    }

    public MovieDetailResponse getMovieDetail(int movieId) {
        Movie movie = movieRepository.findByIdWithPeople(movieId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND,"영화를 찾을 수 없습니다."));

        return MovieConverter.toDetailDTO(movie);
    }
    
    /**
     * 임시 활용
     * 추후 AI 모델 완성되면 해당 AI 와 연동 할 예정
     * */
    public HomeResponseDTO getHomeData() {
        // 1. 오늘의 추천 영화 (평점 1등 영화)
        Movie recommended = movieRepository.findFirstByOrderByVoteAverageDesc()
                .orElseThrow(() -> new RuntimeException("영화 데이터가 없습니다."));

        // 2. DB의 모든 장르 조회
        List<Genre> allGenres = genreRepository.findAll();

        // 3. 모든 장르를 순회하며 각 장르별 영화 10개씩 매핑
        List<HomeResponseDTO.GenreSectionResponse> sections = allGenres.stream()
                .map(genre -> {
                    List<Movie> movies = movieRepository.findTop10ByGenreName(genre.getName(), PageRequest.of(0, 10));

                    if (movies.isEmpty()) return null; // 영화가 없는 장르는 제외

                    return HomeResponseDTO.GenreSectionResponse.builder()
                            .genreName(genre.getName() + " 인기 영화")
                            .movies(movies.stream().map(this::convertToSummary).toList())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return HomeResponseDTO.builder()
                .recommendedMovie(convertToSummary(recommended))
                .sections(sections)
                .build();
    }

    private HomeResponseDTO.MovieSummaryResponse convertToSummary(Movie movie) {
        return HomeResponseDTO.MovieSummaryResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .posterPath(movie.getPosterPath())
                // DB가 10점 만점이라면 2로 나누어 5점 만점으로 변환
                .rating(Math.round((movie.getVoteAverage() / 2.0) * 10) / 10.0)
                .genres(movie.getGenres().stream()
                        .map(mg -> mg.getGenre().getName())
                        .toList())
                .build();
    }
    

}

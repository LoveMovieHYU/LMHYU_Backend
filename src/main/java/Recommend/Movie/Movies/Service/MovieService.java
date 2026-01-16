package Recommend.Movie.Movies.Service;

import Recommend.Movie.Movies.Dto.HomeResponseDTO;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Dto.SearchMovieResponse;
import Recommend.Movie.Movies.Repository.MovieResponseRepository;
import Recommend.Movie.Tmdb.Domain.Genre;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.GenreRepository;
import Recommend.Movie.Tmdb.Repository.MovieRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieResponseRepository movieResponseRepository;
    private final GenreRepository genreRepository;


    public MovieService(MovieRepository movieRepository, MovieResponseRepository movieResponseRepository, GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.movieResponseRepository = movieResponseRepository;
        this.genreRepository = genreRepository;
    }

    public MovieDetailResponse getMovieDetail(int id) {
        Movie movie = movieResponseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("영화를 찾을 수 없습니다. ID: " + id));

        return MovieDetailResponse.from(movie);
    }

    public List<SearchMovieResponse> searchByCategory(String category, String query) {
        // 검색어가 없으면 인기 작품 반환
        if (query == null || query.trim().isEmpty()) {
            return movieResponseRepository.findTop10ByOrderByVoteAverageDesc().stream()
                    .map(SearchMovieResponse::from).toList();
        }

        List<Movie> results;
        // 카테고리에 따른 분기 처리
        switch (category.toLowerCase()) {
            case "title":
                results = movieResponseRepository.findByTitleOnly(query);
                break;
            case "person":
                results = movieResponseRepository.findByPersonOnly(query);
                break;
            case "genre":
                results = movieResponseRepository.findByGenreOnly(query);
                break;
            default: // 카테고리 미지정 시 제목 기반 검색 (이전 로직 활용 가능)
                results = movieResponseRepository.findByTitleOnly(query);
                break;
        }
        return results.stream().map(SearchMovieResponse::from).toList();
    }

    public HomeResponseDTO getHomeData() {
        // 1. 오늘의 추천 영화 (평점 1등 영화)
        Movie recommended = movieResponseRepository.findFirstByOrderByVoteAverageDesc()
                .orElseThrow(() -> new RuntimeException("영화 데이터가 없습니다."));

        // 2. DB의 모든 장르 조회
        List<Genre> allGenres = genreRepository.findAll();

        // 3. 모든 장르를 순회하며 각 장르별 영화 10개씩 매핑
        List<HomeResponseDTO.GenreSectionResponse> sections = allGenres.stream()
                .map(genre -> {
                    List<Movie> movies = movieResponseRepository.findTop10ByGenreName(genre.getName(), PageRequest.of(0, 10));

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

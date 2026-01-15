package Recommend.Movie.Movies.Service;

import Recommend.Movie.Movies.Dto.HomeResponse;
import Recommend.Movie.Tmdb.Domain.Genre;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Repository.GenreRepository;
import Recommend.Movie.Movies.Repository.MovieResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieHomeService {

    private final MovieResponseRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieResponseRepository movieResponseRepository;

    public HomeResponse getHomeData() {
        // 1. 오늘의 추천 영화 (평점 1등 영화)
        Movie recommended = movieRepository.findFirstByOrderByVoteAverageDesc()
                .orElseThrow(() -> new RuntimeException("영화 데이터가 없습니다."));

        // 2. DB의 모든 장르 조회
        List<Genre> allGenres = genreRepository.findAll();

        // 3. 모든 장르를 순회하며 각 장르별 영화 10개씩 매핑
        List<HomeResponse.GenreSectionResponse> sections = allGenres.stream()
                .map(genre -> {
                    List<Movie> movies = movieResponseRepository.findTop10ByGenreName(genre.getName(), PageRequest.of(0, 10));

                    if (movies.isEmpty()) return null; // 영화가 없는 장르는 제외

                    return HomeResponse.GenreSectionResponse.builder()
                            .genreName(genre.getName() + " 인기 영화")
                            .movies(movies.stream().map(this::convertToSummary).toList())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return HomeResponse.builder()
                .recommendedMovie(convertToSummary(recommended))
                .sections(sections)
                .build();
    }

    private HomeResponse.MovieSummaryResponse convertToSummary(Movie movie) {
        return HomeResponse.MovieSummaryResponse.builder()
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
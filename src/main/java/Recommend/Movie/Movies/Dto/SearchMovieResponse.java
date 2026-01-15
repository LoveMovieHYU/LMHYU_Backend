package Recommend.Movie.Movies.Dto;

import Recommend.Movie.Tmdb.Domain.Movie;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchMovieResponse {
    private int id;
    private String title;
    private String posterPath;
    private String releaseYear;
    private String mainGenre; // UI 시안에 표시될 대표 장르 하나

    public static SearchMovieResponse from(Movie movie) {
        // 첫 번째 장르 이름을 대표 장르로 추출
        String genreName = movie.getGenres().isEmpty() ? "장르 없음" :
                movie.getGenres().iterator().next().getGenre().getName();

        return SearchMovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .posterPath(movie.getPosterPath())
                .releaseYear(movie.getReleaseDate() != null ?
                        String.valueOf(movie.getReleaseDate().getYear()) : "미정")
                .mainGenre(genreName)
                .build();
    }
}
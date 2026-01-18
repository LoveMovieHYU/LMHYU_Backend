package Recommend.Movie.Movies.Converter;

import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Domain.Movie;

public class MovieConverter {

    public static MovieSearchResponseDTO toSearchDTO(Movie movie) {
        return MovieSearchResponseDTO.builder()
                .movieId(movie.getId())
                .title(movie.getTitle())
                .posterUrl(movie.getPosterPath())
                .releaseDate(movie.getReleaseDate())
                .build();
    }
}

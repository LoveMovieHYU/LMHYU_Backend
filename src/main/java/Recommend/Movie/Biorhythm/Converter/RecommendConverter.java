package Recommend.Movie.Biorhythm.Converter;

import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendGenresDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDto;
import Recommend.Movie.Tmdb.Domain.Movie;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendConverter {

    public static MovieAiRecommendationDto fromEntity(Movie movie) {

        Set<MovieAiRecommendGenresDTO> genreDtos = (movie.getGenres() == null) ?
                Collections.emptySet() :
                movie.getGenres().stream()
                        .map(movieGenre -> MovieAiRecommendGenresDTO.builder()
                                .genre(movieGenre.getGenre().getName())
                                .build())
                        .collect(Collectors.toSet());

        return MovieAiRecommendationDto.builder()
                .id(movie.getTmdbId())
                .title(movie.getTitle())
                .posterPath(movie.getPosterPath())
                .releaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null)
                .voteAverage(movie.getVoteAverage())
                .genres(genreDtos)
                .build();
    }
}

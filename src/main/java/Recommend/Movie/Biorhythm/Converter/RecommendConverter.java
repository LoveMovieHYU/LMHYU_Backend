package Recommend.Movie.Biorhythm.Converter;

import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendGenresDTO;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDto;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MovieGenre;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecommendConverter {

    public static MovieAiRecommendationDto fromEntity(Movie movie, List<MovieGenre> movieGenres) {

        Set<MovieAiRecommendGenresDTO> genreDtos = (movieGenres == null || movieGenres.isEmpty()) ?
                Collections.emptySet() :
                movieGenres.stream()
                        .map(movieGenre ->
                                new MovieAiRecommendGenresDTO(movieGenre.getGenre().getName()))
                        .collect(Collectors.toSet());

        return MovieAiRecommendationDto.builder()
                .id(movie.getTmdbId())
                .title(movie.getTitle())
                .popularity(movie.getPopularity())
                .posterPath(movie.getPosterPath())
                .releaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null)
                .voteAverage(movie.getVoteAverage())
                .genres(genreDtos)
                .build();
    }
}

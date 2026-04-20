package Recommend.Movie.Movies.Converter;

import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Domain.Job;
import Recommend.Movie.Tmdb.Domain.Movie;
import Recommend.Movie.Tmdb.Domain.MoviePeople;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Dto.PersonDTO;

import java.util.List;

public class MovieConverter {

    public static MovieSearchResponseDTO toSearchDTO(Movie movie) {
        return MovieSearchResponseDTO.builder()
                .tmdbId(movie.getTmdbId())
                .title(movie.getTitle())
                .posterUrl(movie.getPosterPath())
                .releaseDate(movie.getReleaseDate())
                .build();
    }

    public static MovieDetailResponse toDetailDTO(Movie movie, List<MoviePeople> moviePeoples,
                                                  boolean isLiked){

        List<PersonDTO> directors = moviePeoples.stream()
                .map(MoviePeople::getPeople)
                .filter(p -> p.getJob() == Job.DIRECTOR)
                .limit(3)
                .map(PersonDTO::from)
                .toList();

        List<PersonDTO> actors = moviePeoples.stream()
                .map(MoviePeople::getPeople)
                .filter(p -> p.getJob() == Job.ACTOR)
                .limit(10)
                .map(PersonDTO::from)
                .toList();

        return MovieDetailResponse.builder()
                .movieTitle(movie.getTitle())
                .overview(movie.getOverview())
                .releaseYear(movie.getReleaseDate() != null ? String.valueOf(movie.getReleaseDate().getYear()) : "")                .formattedRuntime(formatRuntime(movie.getRuntime()))
                .posterPath(movie.getPosterPath())
                .rating(movie.getVoteAverage())
                .actors(actors)
                .directors(directors)
                .isLiked(isLiked)
                .build();
    }

    private static String formatRuntime(int minutes) {
        if (minutes <= 0) return "";
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}

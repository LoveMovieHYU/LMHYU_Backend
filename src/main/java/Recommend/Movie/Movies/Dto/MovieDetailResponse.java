package Recommend.Movie.Movies.Dto;

import Recommend.Movie.Tmdb.Domain.Job;
import Recommend.Movie.Tmdb.Domain.Movie;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovieDetailResponse {
    private int id;
    private String title;
    private String releaseYear; // 2025
    private String formattedRuntime; // 1h 55m
    private String overview;
    private double rating; // 1.7
    private String posterPath;
    private String backdropPath;

    private List<PersonDTO> directors;
    private List<PersonDTO> actors;

    public static MovieDetailResponse from(Movie movie) {
        // Job(Enum)을 기준으로 감독과 배우 분리
        List<PersonDTO> directors = movie.getPeoples().stream()
                .map(mp -> mp.getPeople()) // MoviePeople에서 People 객체 추출
                .filter(p -> p.getJob() == Job.DIRECTOR)
                .map(PersonDTO::from)
                .toList();

        List<PersonDTO> actors = movie.getPeoples().stream()
                .map(mp -> mp.getPeople())
                .filter(p -> p.getJob() == Job.ACTOR)
                .map(PersonDTO::from)
                .toList();

        return MovieDetailResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .releaseYear(String.valueOf(movie.getReleaseDate().getYear()))
                .formattedRuntime(formatRuntime(movie.getRuntime()))
                .overview(movie.getOverview())
                .rating(movie.getVoteAverage())
                .posterPath(movie.getPosterPath())
                .backdropPath(movie.getBackdropPath())
                .directors(directors)
                .actors(actors)
                .build();
    }

    private static String formatRuntime(int minutes) {
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }
}


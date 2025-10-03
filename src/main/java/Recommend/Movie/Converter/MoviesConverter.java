package Recommend.Movie.Converter;

import Recommend.Movie.DTO.TmdbDTO.MovieDetailDTO;
import Recommend.Movie.Domain.Movie;

import java.util.Objects;


public class MoviesConverter {
    public static Movie toEntity(MovieDetailDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");
        Movie movie = new Movie();
        movie.setTmdbId(dto.getTmdbId());
        movie.setTitle(dto.getTitle());
        movie.setOverview(dto.getOverview());
        movie.setPosterPath(dto.getPosterPath());
        movie.setRuntime(dto.getRuntime());
        movie.setReleaseDate(dto.getReleaseDate());
        movie.setVoteAverage(dto.getVoteAverage() == null ? 0 : (int)Math.round(dto.getVoteAverage()));
        movie.setAdult(dto.getAdult());
        movie.setOriginalLanguage(dto.getOriginalLanguage());
        return movie;

    }

    public static Movie updateFromDTO(Movie movie, MovieDetailDTO dto) {
        if (dto.getTitle() != null) movie.setTitle(dto.getTitle());
        if (dto.getOverview() != null) movie.setOverview(dto.getOverview());
        if (dto.getPosterPath() != null) movie.setPosterPath(dto.getPosterPath());
        if (dto.getRuntime() != null) movie.setRuntime(dto.getRuntime());
        if (dto.getReleaseDate() != null) movie.setReleaseDate(dto.getReleaseDate());
        if (dto.getVoteAverage() != null) movie.setVoteAverage(dto.getVoteAverage());
        if (dto.getAdult() != null) movie.setAdult(dto.getAdult());
        if (dto.getOriginalLanguage() != null) movie.setOriginalLanguage(dto.getOriginalLanguage());
        return movie;
    }

}

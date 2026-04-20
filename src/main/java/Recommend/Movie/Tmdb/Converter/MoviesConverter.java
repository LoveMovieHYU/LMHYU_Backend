package Recommend.Movie.Tmdb.Converter;

import Recommend.Movie.Tmdb.Dto.MovieDetailDTO;
import Recommend.Movie.Tmdb.Domain.Movie;

import java.util.Objects;


public class MoviesConverter {
    public static Movie toEntity(MovieDetailDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");

        return Movie.builder()
                .tmdbId(dto.getTmdbId())
                .title(dto.getTitle())
                .overview(dto.getOverview())
                .voteCount(dto.getVoteCount())
                .posterPath(dto.getPosterPath())
                .runtime(dto.getRuntime())
                .releaseDate(dto.getReleaseDate())
                .voteAverage(dto.getVoteAverage() == null ? 0 : (int)Math.round(dto.getVoteAverage()))
                .adult(dto.getAdult())
                .originalLanguage(dto.getOriginalLanguage())
                .popularity(dto.getPopularity())
                .build();

    }

    public static Movie updateFromDTO(Movie movie, MovieDetailDTO dto) {
        Movie.MovieBuilder b = movie.toBuilder();
        if (dto.getTitle() != null) b.title(dto.getTitle());
        if (dto.getOverview() != null) b.overview(dto.getOverview());
        if (dto.getPosterPath() != null) b.posterPath(dto.getPosterPath());
        if (dto.getRuntime() != null) b.runtime(dto.getRuntime());
        if (dto.getReleaseDate() != null) b.releaseDate(dto.getReleaseDate());
        if (dto.getVoteAverage() != null) b.voteAverage(dto.getVoteAverage());
        if (dto.getAdult() != null) b.adult(dto.getAdult());
        if (dto.getVoteCount() != null) b.voteCount(dto.getVoteCount());
        if (dto.getOriginalLanguage() != null) b.originalLanguage(dto.getOriginalLanguage());
        return b.build();
    }

}

package Recommend.Movie.LikeMovie.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeMovieListResponseDTO {
    @Schema(description = "영화(tmdbId) ID", example = "45")
    private long tmdbId;
    @Schema(description = "영화 제목", example = "어벤져스")
    private String movieTitle;
    @Schema(description = "영화 포스터 경로")
    private String posterPath;
}

package Recommend.Movie.LikeMovie.Domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikeMovieListResponseDTO {
    @Schema(description = "영화 ID", example = "45")
    private int movieId;
    @Schema(description = "영화 제목", example = "어벤져스")
    private String movieTitle;
    @Schema(description = "영화 포스터 경로")
    private String posterPath;
}

package Recommend.Movie.Biorhythm.Dto;

import Recommend.Movie.Movies.Dto.MoviePreviewResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CheckResponseDTO {
    @Schema(title = "캐시 존재 여부", example = "true")
    private boolean isCached;

    @Schema(title = "메시지", example = "추천 받은 영화가 없습니다.")
    private String message;

    @Schema(title = "추천받은 영화 리스트, 없으면 null")
    private List<MoviePreviewResponseDTO> movieList;
}

package Recommend.Movie.Biorhythm.Dto;

import Recommend.Movie.Movies.Dto.MoviePreviewResponseDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CheckResponseDTO {
    private boolean isCached; // 캐시 존재 여부
    private List<MoviePreviewResponseDTO> movieList; // 캐시된 영화 리스트 (없으면 null)
}

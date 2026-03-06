package Recommend.Movie.Movies.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResponseDTO {
    @Schema(title = "영화 ID")
    private long tmdbId;
    
    @Schema(title = "영화 제목")
    private String title;
    
    @Schema(title = "포스터 사진")
    private String posterUrl;

    @Schema(title = "개봉 날짜")
    private LocalDate releaseDate;
}

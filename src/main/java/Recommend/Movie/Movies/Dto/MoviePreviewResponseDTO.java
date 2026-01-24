package Recommend.Movie.Movies.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor // Redis 역직렬화를 위해 필수
@AllArgsConstructor
public class MoviePreviewResponseDTO {

    @Schema(description = "영화 ID", example = "101")
    private int movieId;

    @Schema(description = "영화 제목", example = "인셉션")
    private String title;

    @Schema(description = "포스터 이미지 경로", example = "/path/to/poster.jpg")
    private String posterUrl;

    @Schema(description = "평점", example = "8.8")
    private double rating; // voteAverage

    @Schema(description = "개봉 연도", example = "2010")
    private String releaseYear;

    @Schema(description = "장르 목록", example = "[\"액션\", \"SF\"]")
    private List<String> genres;
}
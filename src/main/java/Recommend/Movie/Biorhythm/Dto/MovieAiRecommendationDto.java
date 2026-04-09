package Recommend.Movie.Biorhythm.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieAiRecommendationDto {
    private Long id;
    private String title;
    private String posterPath; // 포스터 사진
    private String releaseDate; // 개봉 날짜
    private Double voteAverage; // 평점
    private Double popularity; // 인기 지수
    private Set<MovieAiRecommendGenresDTO> genres; // 영화 장르
}

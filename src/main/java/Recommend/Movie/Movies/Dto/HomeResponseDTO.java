package Recommend.Movie.Movies.Dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HomeResponseDTO {
    private MovieSummaryResponse recommendedMovie; // 상단 큰 이미지의 추천 영화
    private List<GenreSectionResponse> sections;  // 아래로 계속 이어지는 장르별 섹션 리스트

    @Getter
    @Builder
    public static class MovieSummaryResponse {
        private int id;
        private String title;
        private String posterPath;
        private double rating; // 5.0 만점 기준 가공
        private List<String> genres; // ["공포", "스릴러"]
    }

    @Getter
    @Builder
    public static class GenreSectionResponse {
        private String genreName; // "액션 인기 영화"
        private List<MovieSummaryResponse> movies; // 해당 장르의 영화들 (가로 리스트용)
    }
}
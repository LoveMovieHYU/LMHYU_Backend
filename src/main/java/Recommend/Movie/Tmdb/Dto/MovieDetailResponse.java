package Recommend.Movie.Tmdb.Dto;

import Recommend.Movie.Movies.Dto.PersonDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MovieDetailResponse {

    @Schema(description = "영화 제목", example = "어벤져스")
    private String movieTitle;

    @Schema(description = "개봉 해", example = "2025")
    private String releaseYear;

    @Schema(description = "영화 제목", example = "1h 55m")
    private String formattedRuntime; // 1h 55m

    @Schema(description = "줄거리", example = "외계인과 싸운다")
    private String overview;
    
    @Schema(description = "영화 평점", example = "4.3")
    private double rating;

    @Schema(description = "영화 포스터", example = "/adgasdg.png")
    private String posterPath;

    @Schema(description = "감독", example = "졸란")
    private List<PersonDTO> directors;

    @Schema(description = "배우", example = "고창석")
    private List<PersonDTO> actors;

}


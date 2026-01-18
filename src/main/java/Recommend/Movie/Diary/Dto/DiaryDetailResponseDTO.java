package Recommend.Movie.Diary.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class DiaryDetailResponseDTO {

    @Schema(description = "오늘의 감정 태그", example = "HAPPY")
    private EmotionTag emotionTag;

    @Schema(description = "일기 제목", example = "인생 영화를 만났다")
    private String title;

    @Schema(description = "영화 제목", example = "어벤져스")
    private String movieTitle;

    @Schema(description = "영화 평점(본인이 남긴것)", example = "4.5")
    private Float rating;

    @Schema(description = "일기 작성 날짜(영화 추천 날짜)", example = "2026-01-15")
    private LocalDate createAt;

    @Schema(description = "포스터 경로", example = "/asdgasdg.jpg")
    private String posterPath;

    @Schema(description = "일기 본문", example = "이번 영화 레전드 꿀잼")
    private String content;
}

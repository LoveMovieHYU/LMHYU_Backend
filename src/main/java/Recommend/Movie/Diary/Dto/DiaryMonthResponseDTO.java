package Recommend.Movie.Diary.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class DiaryMonthResponseDTO {

    @Schema(description = "일기 Id", example = "1")
    private int diaryId;

    @Schema(description = "일기 작성 날짜(영화 추천 날짜)", example = "2026-01-15")
    private LocalDate createAt;
}

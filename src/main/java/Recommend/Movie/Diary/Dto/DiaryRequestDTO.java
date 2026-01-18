package Recommend.Movie.Diary.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiaryRequestDTO {
    @Schema(description = "오늘의 감정 태그", example = "HAPPY")
    @NotNull(message = "감정 태그는 필수입니다.")
    private EmotionTag emotionTag;

    @Schema(description = "일기 제목", example = "인생 영화를 만났다")
    @NotBlank(message = "제목은 필수입니다.") // null, "", " " 모두 허용 안 함
    private String title;

    @Schema(description = "일기 내용", example = "비포 선라이즈는 정말...")
    private String content;

    @Schema(description = "영화 ID (DB에 존재하는 영화)", example = "101")
    @NotNull(message = "영화 ID는 필수입니다.") // ★ 핵심: 여기서 null 체크
    private Integer movieId;

    @Schema(description = "별점 (0.0 ~ 5.0)", example = "4.5")
    @Min(value = 0, message = "별점은 0점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private float rating;
}

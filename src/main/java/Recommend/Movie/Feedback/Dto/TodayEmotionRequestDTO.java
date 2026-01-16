package Recommend.Movie.Feedback.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodayEmotionRequestDTO {
    @Schema(description = "선택할 감정 태그", example = "SAD")
    private EmotionTag emotionTag;
}

package Recommend.Movie.Feedback.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import Recommend.Movie.Movies.Domain.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieReactionRequestDTO {
    @Schema(description = "반응 타입 (LIKE: 좋아요, DISLIKE: 싫어요)", example = "LIKE")
    private ReactionType reactionType;

    @Schema(description = "당시의 감정 상태", example = "HAPPY")
    private EmotionTag emotionTag;
}

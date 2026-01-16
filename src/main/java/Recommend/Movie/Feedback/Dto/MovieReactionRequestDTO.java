package Recommend.Movie.Feedback.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import Recommend.Movie.Movies.Domain.ReactionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieReactionRequestDTO {
    private ReactionType reactionType;
    private EmotionTag emotionTag;
}

package Recommend.Movie.Feedback.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodayEmotionRequestDTO {
    private EmotionTag emotionTag;
}

package Recommend.Movie.Feedback.Dto;

import Recommend.Movie.Diary.Domain.EmotionTag;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodayEmotionResponseDTO {

    private boolean hasSelected;
    private String emotionTag;
    private String message;

    public TodayEmotionResponseDTO(String message, boolean hasSelected, String emotionTag) {
        this.emotionTag = emotionTag;
        this.hasSelected = hasSelected;
        this.message = message;
    }
}

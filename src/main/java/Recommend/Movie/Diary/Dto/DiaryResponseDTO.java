package Recommend.Movie.Diary.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryResponseDTO {
    private String message;

    public DiaryResponseDTO(String message) {
        this.message = message;
    }
}

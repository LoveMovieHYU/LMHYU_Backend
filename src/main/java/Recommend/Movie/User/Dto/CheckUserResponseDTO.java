package Recommend.Movie.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckUserResponseDTO {
    private boolean isChecked;
    @Schema(title = "무엇이 비어있는지 확인 ", example = "NONE\", \"NICKNAME\", \"BIRTHDAY\", \"BOTH\"")
    private String missingField;
    private String message;
}

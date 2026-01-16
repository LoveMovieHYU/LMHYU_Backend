package Recommend.Movie.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record NicknameUpdateRequest(

        @Schema(description = "변경할 새로운 닉네임", example = "movieLover123")
        @NotBlank(message = "닉네임은 필수입니다.")
        String newNickname
) {
}
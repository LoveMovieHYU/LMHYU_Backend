package Recommend.Movie.User.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(

        @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
        @Size(min = 3, max = 13, message = "닉네임은 3자 이상 13자 이하로 설정해야 합니다.")
        String newNickname
) {
}
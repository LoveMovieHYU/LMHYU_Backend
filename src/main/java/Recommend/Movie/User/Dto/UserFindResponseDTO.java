package Recommend.Movie.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UserFindResponseDTO {

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
    @Schema(description = "사용자 닉네임", example = "dlsrks159")
    private String nickname;
    @Schema(description = "사용자 이메일", example = "test@example.com")
    private String email;

    public UserFindResponseDTO(String name, String email, String nickname) {
        this.name = name;
        this.email = email;
        this.nickname = nickname;
    }
}

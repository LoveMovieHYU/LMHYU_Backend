package Recommend.Movie.User.Dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserRequestDTO {

    @Schema(description = "새로운 닉네임", example = "movieLover123")
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickName;

    @Schema(description = "생년월일", example = "2002-08-24")
    @NotBlank(message = "생년월일은 필수입니다.")
    private LocalDate birthday;

}

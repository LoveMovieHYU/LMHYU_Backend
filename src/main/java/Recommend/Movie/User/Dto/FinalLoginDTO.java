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
public class FinalLoginDTO {

    @Schema(description = "새로운 닉네임", example = "movieLover123")
    @NotBlank
    private String nickName;

    @Schema(description = "생년월일", example = "2002-08-24")
    @NotBlank
    private LocalDate birthday;

}

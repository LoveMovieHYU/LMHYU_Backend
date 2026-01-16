package Recommend.Movie.Config.Exception.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ErrorDTO {

    private final String code;
    private final String message;

    private final List<ErrorDetail> errors;
}

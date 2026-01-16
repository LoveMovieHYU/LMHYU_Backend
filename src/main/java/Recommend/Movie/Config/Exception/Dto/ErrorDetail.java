package Recommend.Movie.Config.Exception.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ErrorDetail {

    private final String field;
    private final String message;
}

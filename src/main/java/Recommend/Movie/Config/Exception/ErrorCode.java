package Recommend.Movie.Config.Exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // auth
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    MISSING_COOKIE("MISSING_COOKIE", HttpStatus.BAD_REQUEST),
    DUPLICATE_PHONE("DUPLICATE_PHONE", HttpStatus.CONFLICT),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),

    // service
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    MOVIE_NOT_FOUND("MOVIE_NOT_FOUNT", HttpStatus.NOT_FOUND);

    public final String code;
    public final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

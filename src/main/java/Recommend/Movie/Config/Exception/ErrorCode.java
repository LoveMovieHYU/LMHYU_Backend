package Recommend.Movie.Config.Exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // auth
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    MISSING_COOKIE("MISSING_COOKIE", HttpStatus.BAD_REQUEST),
    DUPLICATE_PHONE("DUPLICATE_PHONE", HttpStatus.CONFLICT),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),

    // validation
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST),
    BIND_FAILED("BIND_FAILED", HttpStatus.BAD_REQUEST),
    MALFORMED_JSON("MALFORMED_JSON", HttpStatus.BAD_REQUEST),

    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE),


    // service
    BAD_REQUEST("BAD_REQUEST",HttpStatus.BAD_REQUEST),
    SAME_NICKNAME("SAME_NICKNAME", HttpStatus.CONFLICT),
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND),
    MOVIE_NOT_FOUND("MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND),
    DIARY_NOT_FOUND("DIARY_NOT_FOUND", HttpStatus.NOT_FOUND),
    LIKE_MOVIE_NOT_FOUND("LIKE_MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND),
    INVALID_INPUT("INVALID_INPUT", HttpStatus.BAD_REQUEST);

    public final String code;
    public final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

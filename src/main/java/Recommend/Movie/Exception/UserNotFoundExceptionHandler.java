package Recommend.Movie.Exception;


public class UserNotFoundExceptionHandler extends RuntimeException {
    public UserNotFoundExceptionHandler(String message) {
        super(message);
    }
}

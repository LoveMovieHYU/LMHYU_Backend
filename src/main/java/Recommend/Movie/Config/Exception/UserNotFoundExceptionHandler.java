package Recommend.Movie.Config.Exception;


public class UserNotFoundExceptionHandler extends RuntimeException {
    public UserNotFoundExceptionHandler(String message) {
        super(message);
    }
}

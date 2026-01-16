package Recommend.Movie.Config.Exception;


public class MovieNotFoundExceptionHandler extends RuntimeException {
    public MovieNotFoundExceptionHandler(String message) {
        super(message);
    }
}

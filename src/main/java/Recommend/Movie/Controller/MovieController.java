package Recommend.Movie.Controller;

import Recommend.Movie.Service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping("/discover")
    public ResponseEntity<?> collectByDiscover(
            @RequestParam(name = "startPage", defaultValue = "1") int startPage,
            @RequestParam(name = "endPage",   defaultValue = "1") int endPage,
            @RequestParam(name = "includeAdult", defaultValue = "false") boolean includeAdult
    ) {
        if (startPage < 1 || endPage < startPage) {
            return ResponseEntity.badRequest().body("잘못된 페이지 범위입니다. startPage는 1 이상, endPage는 startPage 이상이어야 합니다.");
        }

        try {
            movieService.fetchAndSaveAllFromDiscover(startPage, endPage, includeAdult);
            return ResponseEntity.ok(
                    String.format("Discover 수집 완료: startPage=%d, endPage=%d, includeAdult=%s",
                            startPage, endPage, includeAdult)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 필요시 로깅 추가
            return ResponseEntity.internalServerError().body("Discover 수집 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/{movieId}/sync")
    public ResponseEntity<?> syncOne(@PathVariable("movieId") int movieId) {
        if (movieId <= 0) {
            return ResponseEntity.badRequest().body("movieId is required and must be positive.");
        }
        try {
            movieService.fetchAndSaveMovieDetail(movieId);
            return ResponseEntity.ok("success sync: movieId=" + movieId);
        } catch (Exception e) {
            // 필요시 로깅 추가
            return ResponseEntity.internalServerError().body("single sync have error " + e.getMessage());
        }
    }
}

package Recommend.Movie.Controller;

import Recommend.Movie.DTO.MovieListDTO.MovieDetailResponse;
import Recommend.Movie.Service.MovieResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieResponseController {
    private final MovieResponseService movieService;

    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@PathVariable int id) {
        return ResponseEntity.ok(movieService.getMovieDetail(id));
    }
}
package Recommend.Movie.Controller;

import Recommend.Movie.DTO.MovieListDTO.SearchMovieResponse;
import Recommend.Movie.Service.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class MovieSearchController {

    private final MovieSearchService movieSearchService;

    @GetMapping
    public ResponseEntity<List<SearchMovieResponse>> search(
            @RequestParam(defaultValue = "movie") String category,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(movieSearchService.searchByCategory(category, query));
    }
}
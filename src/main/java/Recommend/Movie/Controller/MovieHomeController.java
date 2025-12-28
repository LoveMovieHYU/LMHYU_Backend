package Recommend.Movie.Controller;

import Recommend.Movie.DTO.MovieListDTO.HomeResponse;
import Recommend.Movie.Service.MovieHomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class MovieHomeController {

    private final MovieHomeService movieHomeService;

    @GetMapping
    public ResponseEntity<HomeResponse> getHomeData() {
        return ResponseEntity.ok(movieHomeService.getHomeData());
    }
}
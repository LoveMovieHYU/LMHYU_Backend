package Recommend.Movie.Movies.Controller;

import Recommend.Movie.Movies.Dto.HomeResponseDTO;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Dto.SearchMovieResponse;
import Recommend.Movie.Movies.Service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies") // 베이스 경로 통합
@RequiredArgsConstructor
public class MovieResponseController {

    private final MovieService movieService;

    /**
     * 홈 화면 데이터 조회
     * GET /api/movies/home
     */
    @GetMapping("/home")
    public ResponseEntity<HomeResponseDTO> getHomeData() {
        return ResponseEntity.ok(movieService.getHomeData());
    }

    /**
     * 영화 상세 정보 조회
     * GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@PathVariable int id) {
        return ResponseEntity.ok(movieService.getMovieDetail(id));
    }

    /**
     * 영화 검색
     * GET /api/movies/search?category=
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchMovieResponse>> search(
            @RequestParam(defaultValue = "movie") String category,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(movieService.searchByCategory(category, query));
    }

}
package Recommend.Movie.Movies.Controller;

import Recommend.Movie.Movies.Dto.HomeResponse;
import Recommend.Movie.Movies.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Dto.SearchMovieResponse;
import Recommend.Movie.Movies.Service.MovieHomeService;
import Recommend.Movie.Movies.Service.MovieResponseService;
import Recommend.Movie.Movies.Service.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies") // 베이스 경로 통합
@RequiredArgsConstructor
public class MovieResponseController {

    private final MovieHomeService movieHomeService;
    private final MovieResponseService movieService;
    private final MovieSearchService movieSearchService;

    /**
     * 홈 화면 데이터 조회
     * GET /api/movies/home
     */
    @GetMapping("/home")
    public ResponseEntity<HomeResponse> getHomeData() {
        return ResponseEntity.ok(movieHomeService.getHomeData());
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
     * GET /api/movies/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchMovieResponse>> search(
            @RequestParam(defaultValue = "movie") String category,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(movieSearchService.searchByCategory(category, query));
    }
}
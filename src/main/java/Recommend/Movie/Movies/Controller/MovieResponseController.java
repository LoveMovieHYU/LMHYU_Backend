package Recommend.Movie.Movies.Controller;

import Recommend.Movie.Movies.Dto.HomeResponseDTO;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = " 영화 리스트 조회 API", description = "영화 조회 관련 API")
@RestController
@RequestMapping("/api/movies") // 베이스 경로 통합
public class MovieResponseController {

    private final MovieService movieService;

    public MovieResponseController(MovieService movieService) {
        this.movieService = movieService;
    }

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
     * GET /api/movies/search?keyword=어벤져스&page=1
     */
    @Operation(summary = "영화 검색", description = "영화 제목, 배우, 감독 이름으로 영화를 검색합니다." +
            "다음 페이지로 넘어갈 땐 page 파라미터를 +1 하면 됩니다.")
    @GetMapping("/search")
    public ResponseEntity<List<MovieSearchResponseDTO>> searchMovies(
            @Parameter(description = "검색어 (제목, 배우, 감독)", example = "어벤져스")
            @RequestParam String keyword,
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page) {

        List<MovieSearchResponseDTO> result = movieService.searchMovies(keyword, page);
        return ResponseEntity.ok(result);
    }

}
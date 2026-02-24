package Recommend.Movie.Movies.Controller;

import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Biorhythm.Service.AiDatasetService;
import Recommend.Movie.Biorhythm.Service.RecommendService;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import Recommend.Movie.Movies.Service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = " 영화 리스트 조회 API", description = "영화 조회 관련 API")
@RestController
@Slf4j
@RequestMapping("/api/movies") // 베이스 경로 통합
public class MovieResponseController {

    private final MovieService movieService;
    private final RecommendService recommendService;
    private final AiDatasetService datasetService;

    public MovieResponseController(MovieService movieService, RecommendService recommendService, AiDatasetService datasetService) {
        this.movieService = movieService;
        this.recommendService = recommendService;
        this.datasetService = datasetService;
    }

    /**
     * 영화 상세 정보 조회
     * GET /api/movies?tmdbId=1268552
     */
    @Operation(summary = "영화 상세 정보 조회", description = "영화 ID(tmdbId)를 통해 영화의 제목, 줄거리, 출연진, 평점 등 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MovieDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 영화 ID (ErrorCode: MOVIE_NOT_FOUND)",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@RequestParam long tmdbId, Principal principal) {

        try{
            int userId = Integer.parseInt(principal.getName());

            BiorhythmScore score = recommendService.getOrCalculateBiorhythm(userId);            //AI 서버로 로그 전송 (비동기라 즉시 리턴됨)
            log.info("Sending AI Log - User:{}", userId);
            datasetService.sendInteractionLog(userId, tmdbId, score);

        }catch (Exception e) {
            log.error("AI 학습 데이터 전송 중 오류 발생 (User: {})", principal.getName(), e);
        }

        return ResponseEntity.ok(movieService.getMovieDetail(tmdbId));
    }

    /**
     * 영화 검색
     * GET /api/movies/search?keyword=어벤져스&page=1
     */
    @Operation(summary = "영화 검색", description = "영화 제목, 배우, 감독 이름으로 영화를 검색합니다." +
            "다음 페이지로 넘어갈 땐 page 파라미터를 +1 하면 됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MovieSearchResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 영화 ID (ErrorCode: MOVIE_NOT_FOUND)",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content)
    })
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
package Recommend.Movie.Movies.Controller;

import Recommend.Movie.Biorhythm.Dto.BiorhythmScore;
import Recommend.Movie.Biorhythm.Service.AiDatasetService;
import Recommend.Movie.Biorhythm.Service.RecommendService;
import Recommend.Movie.Movies.Dto.MovieSearchResponseDTO;
import Recommend.Movie.Movies.Service.MovieService;
import Recommend.Movie.Tmdb.Dto.MovieDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Tag(name = "Movie API", description = "Movie lookup API")
@RestController
@Slf4j
@RequestMapping("/api/movies")
public class MovieResponseController {

    private final MovieService movieService;
    private final RecommendService recommendService;
    private final AiDatasetService datasetService;

    public MovieResponseController(MovieService movieService, RecommendService recommendService, AiDatasetService datasetService) {
        this.movieService = movieService;
        this.recommendService = recommendService;
        this.datasetService = datasetService;
    }

    @Operation(summary = "Get movie detail", description = "Returns detailed movie information by tmdbId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(schema = @Schema(implementation = MovieDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @GetMapping("")
    public ResponseEntity<MovieDetailResponse> getMovieDetail(@RequestParam long tmdbId, Principal principal) {
        int userId = -1;

        if (principal != null) {
            try {
                userId = Integer.parseInt(principal.getName());
                BiorhythmScore score = recommendService.getOrCalculateBiorhythm(userId);
                log.info("Sending AI interaction log. user={}, tmdbId={}", userId, tmdbId);
                datasetService.sendInteractionLog(userId, tmdbId, score);
            } catch (Exception e) {
                log.error("AI interaction log failed. user={}", principal.getName(), e);
            }
        }

        return ResponseEntity.ok(movieService.getMovieDetail(tmdbId, userId));
    }

    @Operation(summary = "Search movies", description = "Searches movies by title, actor, or director.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(schema = @Schema(implementation = MovieSearchResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Server error", content = @Content)
    })
    @GetMapping("/search")
    public ResponseEntity<List<MovieSearchResponseDTO>> searchMovies(
            @Parameter(description = "Search keyword", example = "Harry Potter")
            @RequestParam String keyword,
            @Parameter(description = "Page number, starting at 1", example = "1")
            @RequestParam(defaultValue = "1") int page) {

        List<MovieSearchResponseDTO> result = movieService.searchMovies(keyword, page);
        return ResponseEntity.ok(result);
    }
}

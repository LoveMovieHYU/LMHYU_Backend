package Recommend.Movie.Controller;

import Recommend.Movie.DTO.CreditsBatchRequest;
import Recommend.Movie.Service.PeopleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/people")
@Slf4j
public class PeopleController {
    private final PeopleService peopleService;

    public PeopleController(PeopleService peopleService) {
        this.peopleService = peopleService;
    }

    @PostMapping("/movie/{tmdbId}/credits")
    public ResponseEntity<?> fetchCreditsForMovie(
            @PathVariable int tmdbId,
            @RequestParam(defaultValue = "true") boolean fetchDetail
    ) {
        peopleService.fetchAndSaveCreditsByMovieId(tmdbId, fetchDetail);
        return ResponseEntity.ok(
                Map.of(
                        "movieId", tmdbId,
                        "fetchDetail", fetchDetail,
                        "status", "ok"
                )
        );
    }

    /**
     * 배치: 여러 TMDB 영화 ID로 cast/crew 저장
     * 예) POST /api/people/movie/credits:batch  (JSON Body 참고)
     * {
     *   "movieIds": [604079, 550, 603],
     *   "fetchDetail": true
     * }
     */
    @PostMapping("/movie/credits:batch")
    public ResponseEntity<?> fetchCreditsBatch(@RequestBody CreditsBatchRequest body) {
        if (body == null || body.getMovieIds() == null || body.getMovieIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "movieIds is required"));
        }
        boolean fetchDetail = body.getFetchDetail() != null && body.getFetchDetail();

        int success = 0;
        for (Integer id : body.getMovieIds()) {
            try {
                peopleService.fetchAndSaveCreditsByMovieId(id, fetchDetail);
                success++;
            } catch (Exception e) {
                log.warn("credits batch 실패 movieId={}", id, e);
            }
        }
        return ResponseEntity.ok(
                Map.of(
                        "requested", body.getMovieIds().size(),
                        "success", success,
                        "fetchDetail", fetchDetail,
                        "status", "ok"
                )
        );
    }
}

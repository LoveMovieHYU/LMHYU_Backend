package Recommend.Movie.Feedback.Controller;

import Recommend.Movie.Feedback.Service.FeedbackService;
import Recommend.Movie.Movies.Dto.MovieReactionRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     *  추천 영화 반응 저장
     *  POST /api/feedback/{movieId}/reaction
     * */
    @PostMapping("/{movieId}/reaction")
    public ResponseEntity<String> reactionSave(@PathVariable int movieId,
                                               @RequestBody MovieReactionRequestDTO requestDTO,
                                               Principal principal
                                               ){
        String answer = feedbackService.saveMovieReaction(movieId, requestDTO, principal.getName());
        return ResponseEntity.ok(answer);
    }
}

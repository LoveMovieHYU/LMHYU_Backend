package Recommend.Movie.Feedback.Controller;

import Recommend.Movie.Feedback.Dto.TodayEmotionRequestDTO;
import Recommend.Movie.Feedback.Dto.TodayEmotionResponseDTO;
import Recommend.Movie.Feedback.Service.FeedbackService;
import Recommend.Movie.Feedback.Dto.MovieReactionRequestDTO;
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

    /**
     * 오늘의 감정 Redis에 저장 (00시 지나면 초기화)
     * POST /api/feedback/today
     * */
    @PostMapping("/today")
    public ResponseEntity<String> todyEmotionSave(@RequestBody TodayEmotionRequestDTO requestDTO,
                                                  Principal principal){
        String response = feedbackService.saveEmotionRedis(requestDTO,
                Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(response);

    }
    
    /**
     * 오늘의 감정 조회
     * GET /api/feedback/today
     * */
    @GetMapping("/today")
    public ResponseEntity<TodayEmotionResponseDTO> todayEmotion(Principal principal){
        TodayEmotionResponseDTO responseDTO = feedbackService.searchTodayEmotion(
                Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }

}

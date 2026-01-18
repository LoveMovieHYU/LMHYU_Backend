package Recommend.Movie.Feedback.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Feedback.Service.FeedbackService;
import Recommend.Movie.Feedback.Dto.MovieReactionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = " 피드백 & 감정 API", description = "추천된 영화 좋아요/싫어요 반응 및 오늘의 감정 관리 API")
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
    @Operation(summary = "영화 반응(좋아요/싫어요) 저장", description = "추천된 영화에 대해 좋아요/싫어요 반응을 저장합니다. 이미 반응한 경우 수정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공 (반환값: '성공했습니다.' 또는 '반응이 수정됐습니다.')"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 영화 ID")
    })
    @PostMapping("/{movieId}/reaction")
    public ResponseEntity<String> reactionSave(@Parameter(description = "영화 식별자(ID)", example = "123") @PathVariable int movieId,
                                               @RequestBody MovieReactionRequestDTO requestDTO,
                                               Principal principal
                                               ){
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String answer = feedbackService.saveMovieReaction(movieId, requestDTO, principal.getName());

        return ResponseEntity.ok(answer);
    }



}

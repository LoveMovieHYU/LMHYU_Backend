package Recommend.Movie.Emotion.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Emotion.Service.EmotionService;
import Recommend.Movie.Emotion.Dto.TodayEmotionRequestDTO;
import Recommend.Movie.Emotion.Dto.TodayEmotionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "감정 API", description = "오늘의 감정 관리 API")
@RestController
@RequestMapping("/api/emotion")
public class EmotionController {

    private final EmotionService emotionService;

    public EmotionController(EmotionService emotionService) {
        this.emotionService = emotionService;
    }

    /**
     * 오늘의 감정 Redis에 저장 (00시 지나면 초기화)
     * POST /api/feedback/today
     * */
    @Operation(summary = "오늘의 감정 선택(저장)", description = "사용자의 현재 감정을 Redis에 저장합니다. " +
            "저장된 감정은 **매일 자정(00:00 KST)에 자동으로 초기화**됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공 (반환값: '오늘의 감정이 저장되었습니다.')"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @PostMapping("/today")
    public ResponseEntity<String> todyEmotionSave(@RequestBody TodayEmotionRequestDTO requestDTO,
                                                  Principal principal){
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String response = emotionService.saveEmotionRedis(requestDTO,
                Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(response);

    }

    /**
     * 오늘의 감정 조회
     * GET /api/feedback/today
     * */
    @Operation(summary = "오늘의 감정 기록 여부 조회", description = "앱 접속 시, 오늘 이미 감정을 선택했는지 확인합니다." +
            "만약 hasSelected 가 False 면 감정 선택하는 페이지로 이동하면 됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (hasSelected: true/false 반환)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @GetMapping("/today")
    public ResponseEntity<TodayEmotionResponseDTO> todayEmotion(Principal principal){
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        TodayEmotionResponseDTO responseDTO = emotionService.searchTodayEmotion(
                Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }
}

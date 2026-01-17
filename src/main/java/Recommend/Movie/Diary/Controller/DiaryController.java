package Recommend.Movie.Diary.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Diary.Dto.DiaryRequestDTO;
import Recommend.Movie.Diary.Dto.DiaryResponseDTO;
import Recommend.Movie.Diary.Service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /**
     * 감정일기 작성.
     * POST api/diary/
     * */
    @Operation(summary = "영화 일기 작성", description = "DB에 존재하는 영화에 대한 감정 일기를 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 유효성 검사 실패)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저 또는 영화")
    })
    @PostMapping("/")
    public ResponseEntity<DiaryResponseDTO> diaryCreate(@RequestBody @Valid DiaryRequestDTO requestDTO,
                                                        Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        DiaryResponseDTO responseDTO = diaryService.createDiary(requestDTO, Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }
}

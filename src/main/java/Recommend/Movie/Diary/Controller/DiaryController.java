package Recommend.Movie.Diary.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Diary.Dto.*;
import Recommend.Movie.Diary.Service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = " 감정일기 API", description = "감정일기 작성 및 조회")
@RestController
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /**
     * 감정일기 작성.
     * POST api/diary/
     * */
    @Operation(summary = "감정일기 일기 작성", description = "DB에 존재하는 영화에 대한 감정 일기를 작성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 유효성 검사 실패)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저 또는 영화")
    })
    @PostMapping("/")
    public ResponseEntity<DiaryResponseDTO> diaryCreate(@RequestBody @Valid DiaryRequestDTO requestDTO,
                                                        Principal principal) {
        verifyPrincipal(principal);

        DiaryResponseDTO responseDTO = diaryService.createDiary(requestDTO, Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 캘린더 프리뷰 전용
     * 해당 날짜에 적은 일기 프리뷰 조회 가능
     * GET /api/diary/preview?date=2026-01-21
     * */
    @Operation(summary = "감정일기 미리보기 조회", description = "해당 날짜에 작성한 일기의 미리보기를 조회합니다." +
            "캘린더에서 해당 날짜를 눌렀을 때, 미리 보기 기능에 사용")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터 타입 불일치)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저 또는 영화")
    })
    @GetMapping("/preview")
    public ResponseEntity<DiaryPreviewResponseDTO> previewDiary(@RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            , Principal principal) {
        verifyPrincipal(principal);
        DiaryPreviewResponseDTO responseDTO = diaryService.getDiaryPreview(date, Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 캘린더 뷰에 날짜별 데이터
     * GET /api/diary/calendar?date=2026-01
     * */
    @Operation(summary = "특정 달별 작성한 감정일기 반환", description = "특정 달에 작성된 감정 일기들을 반환합니다." +
            "캘린더에 작성된 감정일기를 표시할 때 사용합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터 타입 불일치)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저 또는 영화")
    })
    @GetMapping("/calendar")
    public ResponseEntity<List<DiaryMonthResponseDTO>> monthDiaryList(
            @RequestParam(name = "date") @DateTimeFormat(pattern = "yyyy-MM") YearMonth date,
            Principal principal){
        verifyPrincipal(principal);
        List<DiaryMonthResponseDTO> responseDTO = diaryService.getMontyDiaryList(date, Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 감정일기 상세 보기
     * GET /api/diary/{diaryId}
     * */
    
    @Operation(summary = "감정일기 상세 보기", description = "특정 감정일기 상세보기 조회 API 입니다." )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터 타입 불일치)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 감정일기")
    })
    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryDetailResponseDTO> detailDiary(@PathVariable int diaryId){
        DiaryDetailResponseDTO responseDTO = diaryService.getDetailDiary(diaryId);
        return ResponseEntity.ok(responseDTO);
    }

    private static void verifyPrincipal(Principal principal) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}

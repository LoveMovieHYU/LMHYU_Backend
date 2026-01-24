package Recommend.Movie.Biorhythm.Controller;

import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Dto.CheckResponseDTO;
import Recommend.Movie.Biorhythm.Service.RecommendService;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@Tag(name = "5. 추천 API", description = "바이오리듬 기반 영화 추천 서비스")
@RestController
@RequestMapping("/api/recommend/biorhythm")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 캐시 확인 및 조회 API
     * 프론트엔드 진입 시 가장 먼저 호출
     * - 캐시 있음 -> isCached: true, 리스트 반환
     * - 캐시 없음 -> isCached: false
     */
    @Operation(summary = "추천 내역 확인", description = "오늘 이미 추천받은 내역이 Redis에 있는지 확인하고, 있다면 리스트를 반환합니다." +
            "캐시가 없으면 추후 개발 될 \" 바이오리듬 기반 영화 추천 api\" 호출")
    @GetMapping("/check")
    public ResponseEntity<CheckResponseDTO> checkCachedRecommendation(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

        CheckResponseDTO result = recommendService.checkAndGetCache(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(result);
    }

    /**
     * 바이오리듬 분석 API
     * 생년월일 기반으로 현재 상태 그래프/멘트용 데이터 반환
     */
    @Operation(summary = "바이오리듬 지수 분석", description = "그래프와 멘트 출력을 위한 바이오리듬 지수를 분석하여 반환합니다.")
    @GetMapping("/analyze")
    public ResponseEntity<BiorhythmAnalysisDTO> analyzeBiorhythm(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

        BiorhythmAnalysisDTO result = recommendService.analyzeBiorhythm(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(result);
    }
}
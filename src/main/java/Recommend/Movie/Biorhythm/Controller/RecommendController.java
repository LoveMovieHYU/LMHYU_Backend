package Recommend.Movie.Biorhythm.Controller;

import Recommend.Movie.Biorhythm.Dto.BiorhythmAnalysisDTO;
import Recommend.Movie.Biorhythm.Service.RecommendService;
import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.Biorhythm.Dto.MovieAiRecommendationDto;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;


@Tag(name = "추천 API", description = "바이오리듬 기반 영화 추천 서비스")
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    /**
     * 바이오리듬 분석 API
     * 생년월일 기반으로 현재 상태 그래프/멘트용 데이터 반환
     */
    @Operation(summary = "바이오리듬 지수 분석", description = "그래프와 멘트 출력을 위한 바이오리듬 지수를 분석하여 반환합니다.")
    @GetMapping("/biorhythm/check")
    public ResponseEntity<BiorhythmAnalysisDTO> analyzeBiorhythm(Principal principal) {
        if (principal == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");

        BiorhythmAnalysisDTO result = recommendService.analyzeBiorhythm(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(result);
    }


    /**
     * 신규 회원 AI 추천 요청
     * POST /api/recommend/first/list
     * */

    @Operation(summary = "신규 회원 정보 등록 및 AI 추천", description = "최초 사용자에게 닉네임/생년월일을 입력받아 저장하고," +
            " AI 추천 결과를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 성공", content = @Content(schema = @Schema(implementation = MovieAiRecommendationDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content)
    })
    @PostMapping("/first/list")
    public ResponseEntity<List<MovieAiRecommendationDto>> firstGetBiorhythmRecommendation(@Valid @RequestBody UpdateUserRequestDTO requestDTO,
                                                                                        Principal principal) {
        List<MovieAiRecommendationDto> responseDTO =
                recommendService.savedDetailUserInfoAndRecommend(Integer.parseInt(principal.getName()), requestDTO);

        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 기존 회원 AI 추천 요청
     * GET /api/recommend/list
     * */
    @Operation(summary = "기존 회원 AI 추천 요청", description = "기존 사용자의 정보를 바탕으로 AI 영화 추천을 수행합니다." +
            " (Redis 캐시가 있다면 캐시된 값을 반환합니다.)")
    @GetMapping("/list")
    public ResponseEntity<List<MovieAiRecommendationDto>> getBiorhythmBasedRecommendation(Principal principal) {
        List<MovieAiRecommendationDto> responseDTO =
                recommendService.getbiorhythmBasedRecommendation(Integer.parseInt(principal.getName()));

        return ResponseEntity.ok(responseDTO);
    }

    /**
     * 바이오리듬 커스텀 기반 영화 추천
     * GET /api/recommend/custom/list&p=0.5&e=0.1&i=0.2
     * */
    @GetMapping("/custom/list")
    public ResponseEntity<List<MovieAiRecommendationDto>> getCustomBiorhythmRecommend(
            @RequestParam("p") Double p,
            @RequestParam("e") Double e,
            @RequestParam("i") Double i,
            Principal principal){
        List<MovieAiRecommendationDto> responseDTO =
                recommendService.getCustomRecommend(p, e, i, Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }

}
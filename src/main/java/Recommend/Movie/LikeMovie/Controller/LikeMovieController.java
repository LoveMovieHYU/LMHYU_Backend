package Recommend.Movie.LikeMovie.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.LikeMovie.Domain.LikeMovieListResponseDTO;
import Recommend.Movie.LikeMovie.Service.LikeMovieService;
import Recommend.Movie.LikeMovie.Dto.MovieReactionRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "좋아하는 영화 ", description = "추천된 영화 좋아요")
@RestController
@RequestMapping("/api/likes")
public class LikeMovieController {

    private final LikeMovieService likeMovieService;

    public LikeMovieController(LikeMovieService likeMovieService) {
        this.likeMovieService = likeMovieService;
    }

    /**
     *  추천 영화 반응 저장
     *  POST /api/likes/{movieId}
     * */
    @Operation(summary = "영화 좋아요 저장", description = "추천된 영화에 대해 좋아요를 저장합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공 (반환값: '성공했습니다.')"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 영화 ID")
    })
    @PostMapping("/{movieId}")
    public ResponseEntity<String> reactionSave(@Parameter(description = "영화 식별자(ID)", example = "123") @PathVariable int movieId,
                                               @RequestBody MovieReactionRequestDTO requestDTO,
                                               Principal principal
                                               ){
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String answer = likeMovieService.saveMovieReaction(movieId, requestDTO, principal.getName());

        return ResponseEntity.ok(answer);
    }

    /**
     * 좋아요한 영화 조회
     * GET /api/likes
     * */
    @Operation(summary = "최근 좋아요한 영화 조회", description = "유저가 '좋아요'를 누른 영화 리스트 반환 ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    @GetMapping("/")
    public ResponseEntity<List<LikeMovieListResponseDTO>> likeMovieList(Principal principal){
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        List<LikeMovieListResponseDTO> responseDTO = likeMovieService.getLikeMovieList(principal.getName());
        return ResponseEntity.ok(responseDTO);
    }

}

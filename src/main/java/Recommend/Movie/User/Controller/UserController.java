package Recommend.Movie.User.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Dto.NicknameUpdateRequest;
import Recommend.Movie.User.Dto.UserFindResponseDTO;
import Recommend.Movie.User.Service.UserService;
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

@Tag(name = "유저(마이페이지) API", description = "유저 정보 조회, 닉네임 수정, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 유저 정보 조회
     * GET /api/user
     * */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 상세 정보(이름, 이메일 등)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = UserFindResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요 (토큰 만료 or 없음)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저")
    })
    @GetMapping(value = "/")
    public UserFindResponseDTO userMeApi(Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userService.readUser(Integer.parseInt(principal.getName()));
    }

    /**
     * 유저 삭제
     * DELETE /api/user
     * */
    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자의 계정을 삭제하고, " +
            "관련 Refresh Token을 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 성공 (true 반환)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "이미 삭제되었거나 없는 유저")
    })
    @DeleteMapping(value = "/")
    public ResponseEntity<Boolean> deleteUserApi(Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        userService.deleteUser(Integer.parseInt(principal.getName()));
        return ResponseEntity.status(200).body(true);
    }

    /**
     * 닉네임 변경
     * PUT /api/user/nickname
     * */
    @Operation(summary = "닉네임 변경", description = "사용자의 닉네임을 변경합니다. 중복 체크 및 동일 닉네임 방지 로직이 포함되어 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공 (Nickname updated successfully.)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (현재 닉네임과 동일함)"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "409", description = "충돌 (이미 존재하는 닉네임)"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @PutMapping(value = "/nickname")
    public ResponseEntity<String> updateUserNickname(@Valid @RequestBody NicknameUpdateRequest request,
                                                     Principal principal) {

        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        userService.updateNickname(Integer.parseInt(principal.getName()), request.newNickname());
        return ResponseEntity.ok("Nickname updated successfully.");

    }
}
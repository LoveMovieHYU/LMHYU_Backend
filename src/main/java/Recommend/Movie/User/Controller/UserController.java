package Recommend.Movie.User.Controller;

import Recommend.Movie.Config.Exception.BusinessException;
import Recommend.Movie.Config.Exception.ErrorCode;
import Recommend.Movie.User.Dto.CheckUserResponseDTO;
import Recommend.Movie.User.Dto.FinalLoginDTO;
import Recommend.Movie.User.Dto.UpdateUserRequestDTO;
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

@Tag(name = "유저 API", description = "유저 정보 조회, 닉네임&생년월일 수정, 회원 탈퇴 API")
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
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 상세 정보(이름, 이메일, 닉네임 등)를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = UserFindResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "로그인 필요 (토큰 만료 or 없음)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 유저")
    })
    @GetMapping(value = "/")
    public ResponseEntity<UserFindResponseDTO> userMeApi(Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        UserFindResponseDTO responseDTO = userService.readUser(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
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
     * 유저 정보 변경
     * PUT /api/user/update
     * */

    @Operation(summary = "유저 정보 업데이트 (소셜 로그인 시, 최초 정보 입력)", description = "사용자의 닉네임과 " +
            "생년월일을 변경합니다.(소셜 로그인 시, 최초 정보 입력)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공 "),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 "),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @PostMapping("/update")
    public ResponseEntity<String> loginFinalUser(@Valid @RequestBody FinalLoginDTO requestDTO,
                                             Principal principal) {

        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        String response = userService.loginUserUpdate(Integer.parseInt(principal.getName()), requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * 유저 정보 변경
     * PUT /api/user/update
     * 닉네임은 중복될 수 없으며, 생년월일을 변경하면 바이오리듬과 영화 리스트 캐시가 삭제됩니다.
     * */

    @Operation(summary = "유저 정보 업데이트", description = "사용자의 닉네임과 생년월일을 변경합니다." +
            "닉네임은 중복될 수 없으며, 생년월일을 변경하면 바이오리듬과 영화 리스트 캐시가 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공 "),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 "),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @PatchMapping("/update")
    public ResponseEntity<String> updateUser(@Valid @RequestBody UpdateUserRequestDTO requestDTO,
                                                     Principal principal) {

        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String response = userService.updateUserInfo(Integer.parseInt(principal.getName()), requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 필수 정보(닉네임/생일) 입력 여부 확인
     * GET /api/user/check-profile
     * */
    @Operation(summary = "회원 필수 정보(닉네임/생일) 입력 여부 확인",
            description = "소셜 로그인 후, 닉네임과 생년월일이 저장되어 있는지 확인합니다. \n" +
                    "isChecked가 false라면 응답의 missingField를 확인하여 해당 입력 페이지로 이동해야 합니다." +
                    "isChecked: true $ 메인 화면으로 이동 " +
                    "isChecked: false  정보 입력 화면으로 이동")
    @GetMapping("/check-profile")
    public ResponseEntity<CheckUserResponseDTO> checkUserBirthDayNickName(Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        CheckUserResponseDTO responseDTO = userService.checkUserInfo(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(responseDTO);
    }


    /**
     * 유저 로그아웃
     * GET /api/user/logout
     * */
    @Operation(summary = "유저 로그아웃, 리프레시 토큰 삭제")
    @GetMapping("/logout")
    public ResponseEntity<String> logoutUser(Principal principal) {
        if(principal == null){
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String response = userService.logoutUser(Integer.parseInt(principal.getName()));
        return ResponseEntity.ok(response);
    }
}
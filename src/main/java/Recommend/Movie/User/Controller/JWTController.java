package Recommend.Movie.User.Controller;

import Recommend.Movie.User.Dto.JWTResponseDTO;
import Recommend.Movie.User.Service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "JWT 토큰", description = "JWT 토큰 재발급 API")
@RestController
@RequestMapping("/api/jwt")
public class JWTController {

    private final JwtService jwtService;

    public JWTController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Refresh 토큰으로 Access 토큰 재발급 (Rotate 포함)
     * POST /api/jwt/refresh
     */
    @Operation(summary = "Access 토큰 재발급",
            description = "Refresh 토큰으로 Access 토큰을 재발급합니다. (Refresh Token Rotation 포함)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = JWTResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh 토큰")
    })
    @PostMapping("/refresh")
    public ResponseEntity<JWTResponseDTO> jwtRefreshApi(
            @Parameter(description = "'Authorization-Refresh' 헤더에 담겨 오는 리프레시 토큰", required = true)
            @RequestHeader("Authorization-Refresh") String refreshToken
    ) {
        JWTResponseDTO newTokens = jwtService.refreshRotate(refreshToken);
        return ResponseEntity.ok(newTokens);
    }
}

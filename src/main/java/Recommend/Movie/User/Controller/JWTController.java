package Recommend.Movie.User.Controller;

import Recommend.Movie.User.Dto.JWTResponseDTO;
import Recommend.Movie.User.Service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JWTController {

    private final JwtService jwtService;

    public JWTController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Refresh 토큰으로 Access 토큰 재발급 (Rotate 포함)
     * @param refreshToken 'Authorization-Refresh' 헤더에 담겨 오는 리프레시 토큰
     */
    @PostMapping(value = "/jwt/refresh")
    public ResponseEntity<?> jwtRefreshApi(
            @RequestHeader("Authorization-Refresh") String refreshToken
    ) {
        try {
            JWTResponseDTO newTokens = jwtService.refreshRotate(refreshToken);
            return ResponseEntity.ok(newTokens);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
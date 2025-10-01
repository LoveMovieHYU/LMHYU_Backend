package Recommend.Movie.Controller;

import Recommend.Movie.DTO.GoogleLoginRequest;
import Recommend.Movie.DTO.JWTResponseDTO;
import Recommend.Movie.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// IOException, GeneralSecurityException import는 더 이상 필요 없습니다.

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<JWTResponseDTO> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        // 👇 try-catch 블록의 catch 부분을 수정합니다.
        try {
            JWTResponseDTO tokens = authService.loginWithGoogle(request.getIdToken());
            return ResponseEntity.ok(tokens);
        } catch (Exception e) { // 👈 GeneralSecurityException | IOException 대신 Exception 으로 변경
            // AuthService에서 던진 RuntimeException을 여기서 잡습니다.
            e.printStackTrace();
            return ResponseEntity.status(401).build(); // Unauthorized
        }
    }
}
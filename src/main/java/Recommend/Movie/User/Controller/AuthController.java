package Recommend.Movie.User.Controller;

import Recommend.Movie.User.Dto.GoogleLoginRequest;
import Recommend.Movie.User.Dto.LoginResponseDTO;
import Recommend.Movie.User.Dto.NaverLoginRequest;
import Recommend.Movie.User.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponseDTO> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        try {
            LoginResponseDTO response = authService.loginWithGoogle(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/naver")
    public ResponseEntity<LoginResponseDTO> loginWithNaver(@RequestBody NaverLoginRequest request) {
        try {
            LoginResponseDTO response = authService.loginWithNaver(request.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).build();
        }
    }
}
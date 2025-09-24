package Recommend.Movie.Login.Controller;

import Recommend.Movie.Login.DTO.AuthResponse;
import Recommend.Movie.Login.DTO.GoogleLoginRequest;
import Recommend.Movie.Login.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        try {
            AuthResponse authResponse = authService.verifyGoogleIdToken(request.getIdToken());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(null);
        }
    }

}

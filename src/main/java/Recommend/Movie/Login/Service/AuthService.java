package Recommend.Movie.Login.Service;


import Recommend.Movie.Login.Util.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import Recommend.Movie.Login.Domain.LoginUser;
import Recommend.Movie.Login.DTO.AuthResponse;
import Recommend.Movie.Login.Repository.LoginUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Collections;

@Service
public class AuthService {

    @Value("${google.client-id}")
    private String googleClientId;
    private final LoginUserRepository loginUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(LoginUserRepository loginUserRepository, JwtTokenProvider jwtTokenProvider) {
        this.loginUserRepository = loginUserRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Transactional
    public AuthResponse verifyGoogleIdToken(String idTokenString) throws Exception {
        System.out.println("Backend Configured Google Client ID: " + googleClientId);
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Invalid ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String gender = (String) payload.get("gender");
        String birthdate = (String) payload.get("birthdate");

        System.out.println("Verified Email: " + email);
        System.out.println("Verified Name: " + name);
        System.out.println("Verified Gender: " + gender);
        System.out.println("Verified Birthdate: " + birthdate);

        LoginUser loginUser = loginUserRepository.findByEmail(email)
                .orElseGet(() -> {
                    LoginUser newLoginUser = new LoginUser(email, name, gender, birthdate);
                    return loginUserRepository.save(newLoginUser);
                });

        String accessToken = jwtTokenProvider.generateToken(loginUser);
        return new AuthResponse(accessToken);
    }

}

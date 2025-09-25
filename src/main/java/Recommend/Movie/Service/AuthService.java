package Recommend.Movie.Service;


import Recommend.Movie.Domain.User;
import Recommend.Movie.Util.JwtTokenProvider;
import Recommend.Movie.Repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import Recommend.Movie.DTO.AuthResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
public class AuthService {

    @Value("${google.client-id}")
    private String googleClientId;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
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

        log.info("Verified Google ID Token for email: " + email);

        User user = userRepository.findByEmail(email);

        String accessToken = jwtTokenProvider.generateToken(user);
        return new AuthResponse(accessToken);
    }

}

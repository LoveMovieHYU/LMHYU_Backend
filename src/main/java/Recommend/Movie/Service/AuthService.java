package Recommend.Movie.Service;

import Recommend.Movie.Converter.UserConverter;
import Recommend.Movie.DTO.JWTResponseDTO;
import Recommend.Movie.DTO.UserDTO.LoginDTO;
import Recommend.Movie.Domain.SocialProviderType;
import Recommend.Movie.Domain.User;
import Recommend.Movie.Domain.UserRoleType;
import Recommend.Movie.Repository.UserRepository;
import Recommend.Movie.Util.JWTUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;
    private final WebClient webClient;


    public AuthService(@Value("${GOOGLE_CLIENT_ID}") String googleClientId,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;


        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        this.webClient = WebClient.builder().build();
    }

    @Transactional
    public JWTResponseDTO loginWithGoogle(String idTokenString) {
        // ... (기존 구글 로그인 코드는 변경 없음)
        try {
            log.info(">>>>>> Google ID 토큰 검증 시작...");
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("ID Token is invalid or expired.");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleUniqueId = payload.getSubject();
            String email = payload.getEmail();
            String providerId = "GOOGLE_" + googleUniqueId;
            String name = (String) payload.get("name");

            User user = userRepository.findByProviderIdAndIsSocial(providerId, true).orElseGet(() -> {
                LoginDTO dto = LoginDTO.builder()
                        .name(name).providerId(providerId).email(email)
                        .isSocial(true).isLock(false)
                        .socialProviderType(SocialProviderType.GOOGLE).role(UserRoleType.USER)
                        .build();
                return userRepository.save(UserConverter.toEntity(dto));
            });

            String accessToken = JWTUtil.createJWT(user.getName(), "ROLE_" + user.getRoleType().name(), true);
            String refreshToken = JWTUtil.createJWT(user.getName(), "ROLE_" + user.getRoleType().name(), false);
            jwtService.addRefresh(user.getName(), refreshToken);
            return new JWTResponseDTO(accessToken, refreshToken);
        } catch (Exception e) {
            log.error("!!!!!! Google 로그인 처리 중 예외 발생 !!!!!!", e);
            throw new RuntimeException("Login processing failed", e);
        }
    }

    @Transactional
    public JWTResponseDTO loginWithNaver(String accessToken) {
        try {
            // 1. 네이버 액세스 토큰으로 사용자 정보 조회
            log.info(">>>>>> 네이버 사용자 정보 조회 시작 (Access Token 방식)...");
            String response = webClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response, new TypeReference<>() {});

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) responseMap.get("response");

            if (userInfo == null) {
                throw new IllegalArgumentException("Invalid Naver Token or failed to parse user info.");
            }
            log.info(">>>>>> 네이버 사용자 정보 조회 성공!");

            // 2. 사용자 정보 추출
            String naverUniqueId = (String) userInfo.get("id");
            String email = (String) userInfo.get("email");
            String providerId = "NAVER_" + naverUniqueId;
            String name = (String) userInfo.get("name");

            // 3. DB에서 사용자 조회 또는 신규 생성
            User user = userRepository.findByProviderIdAndIsSocial(providerId, true).orElseGet(() -> {
                LoginDTO dto = LoginDTO.builder()
                        .name(name).providerId(providerId).email(email)
                        .isSocial(true).isLock(false)
                        .socialProviderType(SocialProviderType.NAVER).role(UserRoleType.USER)
                        .build();
                return userRepository.save(UserConverter.toEntity(dto));
            });

            // 4. 우리 서비스의 JWT 생성 및 반환
            String ourAccessToken = JWTUtil.createJWT(user.getName(), "ROLE_" + user.getRoleType().name(), true);
            String ourRefreshToken = JWTUtil.createJWT(user.getName(), "ROLE_" + user.getRoleType().name(), false);
            jwtService.addRefresh(user.getName(), ourRefreshToken);
            log.info(">>>>>> JWT 생성 및 저장 완료!");

            return new JWTResponseDTO(ourAccessToken, ourRefreshToken);

        } catch (Exception e) {
            log.error("!!!!!! naver 로그인 처리 중 예외 발생 !!!!!!", e);
            throw new RuntimeException("Naver login processing failed", e);
        }
    }
}
package Recommend.Movie.Service;

import Recommend.Movie.Converter.UserConverter;
import Recommend.Movie.DTO.JWTResponseDTO;
import Recommend.Movie.DTO.UserDTO.LoginDTO;
import Recommend.Movie.Domain.SocialProviderType;
import Recommend.Movie.Domain.User;
import Recommend.Movie.Domain.UserRoleType;
import Recommend.Movie.Repository.UserRepository;
import Recommend.Movie.Util.JWTUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier verifier;

    public AuthService(@Value("${GOOGLE_CLIENT_ID}") String googleClientId,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Transactional
    public JWTResponseDTO loginWithGoogle(String idTokenString) {
        try {
            // 1. 구글 ID 토큰 검증
            log.info(">>>>>> Google ID 토큰 검증 시작...");
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.error(">>>>>> ID Token 검증 실패: 토큰이 null이거나 유효하지 않습니다.");
                throw new IllegalArgumentException("ID Token is invalid or expired.");
            }
            log.info(">>>>>> Google ID 토큰 검증 성공!");
            GoogleIdToken.Payload payload = idToken.getPayload();

            // 2. 기본 사용자 정보 추출
            String googleUniqueId = payload.getSubject();
            String email = payload.getEmail();
            String providerId = "GOOGLE_" + googleUniqueId;
            String name = (String) payload.get("name");
            log.info(">>>>>> 기본 사용자 정보 추출 완료: {}", name);

            // --- People API 호출 및 추가 정보 처리 로직 전체 삭제 ---

            // 3. DB에서 providerId로 사용자 조회, 없으면 새로 생성
            User user = userRepository.findByProviderIdAndIsSocial(providerId, true).orElseGet(() -> {
                log.info(">>>>>> 신규 사용자입니다. DTO를 통해 DB에 저장합니다: {}", providerId);

                // --- 요청하신 수정사항 시작 ---

                // 3-1. LoginDTO에 신규 사용자 정보 1차 저장
                LoginDTO dto = LoginDTO.builder()
                        .name(name)
                        .providerId(providerId)
                        .email(email)
                        .isSocial(true)
                        .isLock(false)
                        .socialProviderType(SocialProviderType.GOOGLE)
                        .role(UserRoleType.USER)
                        .build();

                // 3-2. UserConverter의 toEntity 메소드를 사용해 User 엔티티로 변환
                User newUser = UserConverter.toEntity(dto);

                // 3-3. 변환된 엔티티를 DB에 저장
                return userRepository.save(newUser);

            });

            // 4. 우리 서비스의 JWT 생성
            String role = user.getRoleType().name();
            String accessToken = JWTUtil.createJWT(user.getName(), "ROLE_" + role, true);
            String refreshToken = JWTUtil.createJWT(user.getName(), "ROLE_" + role, false);

            // 5. 리프레시 토큰을 DB에 저장/업데이트
            jwtService.addRefresh(user.getName(), refreshToken);
            log.info(">>>>>> JWT 생성 및 저장 완료! 성공적으로 토큰을 반환합니다.");

            return new JWTResponseDTO(accessToken, refreshToken);

        } catch (Exception e) {
            log.error("!!!!!! Google 로그인 처리 중 심각한 예외 발생 !!!!!!", e);
            throw new RuntimeException("Login processing failed", e);
        }
    }
}
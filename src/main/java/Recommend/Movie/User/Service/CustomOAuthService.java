package Recommend.Movie.User.Service;

import Recommend.Movie.User.Converter.UserConverter;
import Recommend.Movie.User.Domain.SocialProviderType;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Domain.UserRoleType;
import Recommend.Movie.User.Dto.LoginDTO;
import Recommend.Movie.User.Repository.CustomOAuth2User; // CustomOAuth2User 클래스가 있다고 가정
import Recommend.Movie.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * 소셜 로그인 (구글/네이버 등) 성공 시 호출
     * 역할: 소셜 유저 정보 로드 -> DB 저장/업데이트 -> SecurityContext에 저장할 Principal 반환
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase(); // GOOGLE, NAVER

        String providerId = "";
        String email = "";
        String name = "";
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if (SocialProviderType.NAVER.name().equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            providerId = "NAVER_" + response.get("id");
            email = String.valueOf(response.get("email"));
            name = String.valueOf(response.get("name"));
        }
        else if (SocialProviderType.GOOGLE.name().equals(registrationId)) {
            providerId = "GOOGLE_" + attributes.get("sub");
            email = String.valueOf(attributes.get("email"));
            name = String.valueOf(attributes.get("name"));
        }
        else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        User user = saveOrUpdateUser(providerId, email, name, registrationId);

        Map<String, Object> finalAttributes = new HashMap<>(attributes);
        finalAttributes.put("providerId", providerId);

        return new CustomOAuth2User(
                finalAttributes,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRoleType().name())),
                name,
                providerId
        );
    }

    private User saveOrUpdateUser(String providerId, String email, String name, String registrationId) {
        Optional<User> userOptional = userRepository.findByProviderIdAndIsSocial(providerId, true);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();
            // existingUser.updateEmail(email);
            return existingUser;
        } else {
            LoginDTO loginDTO = LoginDTO.builder()
                    .providerId(providerId)
                    .email(email)
                    .name(name)
                    .isSocial(true)
                    .isLock(false)
                    .role(UserRoleType.USER)
                    .socialProviderType(SocialProviderType.valueOf(registrationId))
                    .build();

            return userRepository.save(UserConverter.toEntity(loginDTO));
        }
    }
}
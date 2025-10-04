package Recommend.Movie.Service;


import Recommend.Movie.Converter.UserConverter;
import Recommend.Movie.DTO.CustomOAuth2User;
import Recommend.Movie.DTO.UserDTO.LoginDTO;
import Recommend.Movie.DTO.UserDTO.UpdateDTO;
import Recommend.Movie.DTO.UserDTO.UserFindResponseDTO;
import Recommend.Movie.Domain.SocialProviderType;
import Recommend.Movie.Domain.User;
import Recommend.Movie.Domain.UserRoleType;
import Recommend.Movie.Exception.UserNotFoundExceptionHandler;
import Recommend.Movie.Repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService extends DefaultOAuth2UserService {

private final UserRepository userRepository;
private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(int userId) {
        try{
            User user = userRepository.findByUserId(userId);
            jwtService.removeRefreshUser(user.getName());
            // 유저 삭제
            userRepository.delete(user);
        } catch (UserNotFoundExceptionHandler ex){
            throw new UserNotFoundExceptionHandler("유저를 찾을 수 없습니다.");
        }
    }

    // 소셜 로그인 (매 로그인시 : 신규 = 가입, 기존 = 업데이트)
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // 부모 메소드 호출
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 데이터
        Map<String, Object> attributes;
        List<GrantedAuthority> authorities;

        String name;
        String role = UserRoleType.USER.name();
        String email;
        String providerId;


        // provider 제공자별 데이터 획득
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        if (registrationId.equals(SocialProviderType.NAVER.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            name = attributes.get("name").toString();
            providerId = registrationId + "_" + attributes.get("id");
            email = attributes.get("email").toString();


        } else if (registrationId.equals(SocialProviderType.GOOGLE.name())) {

            attributes = (Map<String, Object>) oAuth2User.getAttributes();
            name = attributes.get("name").toString();
            providerId = registrationId + "_" + attributes.get("sub");
            email = attributes.get("email").toString();


        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // 데이터베이스 조회 -> 존재하면 업데이트, 없으면 신규 가입
        Optional<User> entity = userRepository.findByProviderIdAndIsSocial(providerId, true);
        if (entity.isPresent()) {
            // role 조회
            role = entity.get().getRoleType().name();

            // 기존 유저 업데이트
            UpdateDTO dto = new UpdateDTO(providerId, email);
            userRepository.save(UserConverter.updateUser(dto));
        } else {
            LoginDTO dto = LoginDTO.builder()
                            .name(name)
                            .providerId(providerId)
                            .isLock(false)
                            .isSocial(true)
                            .socialProviderType(SocialProviderType.valueOf(registrationId))
                            .role(UserRoleType.USER)
                            .email(email)
                            .build();
            userRepository.save(UserConverter.toEntity(dto));
        }

        authorities = List.of(new SimpleGrantedAuthority(role));

        return new CustomOAuth2User(attributes, authorities, name);
    }


    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserFindResponseDTO readUser(int userId) {
        try{
            User user = userRepository.findByUserId(userId);
            UserFindResponseDTO responseDTO = new UserFindResponseDTO(user.getName(), user.getEmail());
            return responseDTO;
        } catch (UserNotFoundExceptionHandler ex){
            throw new UserNotFoundExceptionHandler("User not found.");
        }
    }
}
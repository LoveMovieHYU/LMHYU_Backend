package Recommend.Movie.User.Handler;

import Recommend.Movie.User.Repository.CustomOAuth2User;
import Recommend.Movie.User.Domain.User;
import Recommend.Movie.User.Repository.UserRepository;
import Recommend.Movie.User.Service.JwtService;
import Recommend.Movie.Util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public OAuth2LoginSuccessHandler(JWTUtil jwtUtil, JwtService jwtService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 Login 성공! 토큰 생성 및 리다이렉트 준비");
        String name = authentication.getName();
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String providerId = (String) oAuth2User.getAttributes().get("providerId");

        User user = userRepository.findByProviderIdAndIsSocial(providerId, true)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다."));

        String accessToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), true);
        String refreshToken = JWTUtil.createJWT(String.valueOf(user.getUserId()), "ROLE_" + user.getRoleType().name(), false);

        // Refresh 화이트리스트 저장
        jwtService.addRefresh(name, refreshToken);


        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/test/oauth/callback") // 여기를 수정!
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

//
//        // 안드로이드 앱으로 리다이렉트 (Deep Link)
//        // 스키마(scheme)는 프론트 팀과 상의해서 정해야 함 (여기선 example-app://callback 으로 가정)
//        String targetUrl = UriComponentsBuilder.fromUriString("example-app://callback")
//                .queryParam("accessToken", accessToken)
//                .queryParam("refreshToken", refreshToken)
//                .build().toUriString();

        log.info("Redirecting to: {}", targetUrl);

        // 부모 클래스의 메서드를 사용하여 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
package Recommend.Movie.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Recommend.Movie.Service.JwtService;
import Recommend.Movie.Util.JWTUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Qualifier("SocialSuccessHandler")
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    public SocialSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String name = authentication.getName();
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        String accessToken  = JWTUtil.createJWT(name, "ROLE_" + role, true);   // 짧은 만료
        String refreshToken = JWTUtil.createJWT(name, "ROLE_" + role, false);  // 긴 만료

        // Refresh 화이트리스트 저장
        jwtService.addRefresh(name, refreshToken);

        // JSON 응답(리다이렉트/쿠키 X)
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        String body = """
      {"accessToken":"%s","refreshToken":"%s","tokenType":"Bearer","expiresIn":%d}
      """.formatted(accessToken, refreshToken, /*access 만료 초*/ 3600);
        response.getWriter().write(body);
    }

}
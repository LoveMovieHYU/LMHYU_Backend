package Recommend.Movie.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Recommend.Movie.Service.JwtService;
import Recommend.Movie.Util.JWTUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;

    public RefreshTokenLogoutHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // 1. 요청의 body를 읽던 로직을 모두 삭제합니다.
        // try-catch 문도 필요 없어져서 코드가 깔끔해집니다.

        // 2. 대신 'Authorization-Refresh'라는 이름의 헤더에서 리프레시 토큰을 가져옵니다.
        String refreshToken = request.getHeader("Authorization-Refresh");

        // 유효성 검증
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            return;
        }

        // Refresh 토큰 삭제
        jwtService.removeRefresh(refreshToken);
    }
}

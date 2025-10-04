package Recommend.Movie.handler;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Recommend.Movie.Service.JwtService;
import Recommend.Movie.Util.JWTUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;


public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;

    public RefreshTokenLogoutHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {

        String refreshToken = request.getHeader("Authorization-Refresh");

        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        Boolean isValid = JWTUtil.isValid(refreshToken, false);
        if (!isValid) {
            return;
        }

        jwtService.removeRefresh(refreshToken);
    }
}

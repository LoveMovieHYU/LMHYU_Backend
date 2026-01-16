package Recommend.Movie.User.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    // SuccessHandler가 이 주소로 리다이렉트 시켜주면 브라우저에 토큰을 출력함
    @GetMapping("/test/oauth/callback")
    public String oauthCallback(@RequestParam String accessToken,
                                @RequestParam String refreshToken) {
        return "<h1>로그인 성공! 🎉</h1>" +
                "<h3>Access Token:</h3>" +
                "<p style='word-break: break-all;'>" + accessToken + "</p>" +
                "<h3>Refresh Token:</h3>" +
                "<p style='word-break: break-all;'>" + refreshToken + "</p>";
    }
}
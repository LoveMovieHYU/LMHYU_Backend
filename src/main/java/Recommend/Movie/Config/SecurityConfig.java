package Recommend.Movie.Config;


import Recommend.Movie.User.Handler.OAuth2LoginSuccessHandler;
import Recommend.Movie.User.Service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final UserService userService;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, UserService userService) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF, Form Login, HTTP Basic 인증 비활성화
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // 경로별 인가 작업
        http
                .authorizeHttpRequests(auth -> auth
                        // 새로 만든 구글 로그인 API와 JWT 재발급 API를 인증 없이 허용
                        .requestMatchers("/api/v1/auth/google", "/jwt/refresh", "/api/v1/auth/naver","/api/movies/**","/api/home", "/api/search/**").permitAll()
                        .requestMatchers("/", "/login-test.html", "/test/**").permitAll() // 테스트 경로 허용
                        .requestMatchers("/api/v1/auth/**", "/oauth2/**").permitAll()
                        .requestMatchers("api/feedback/**").permitAll()
                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                );

        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfo) -> userInfo
                                .userService(userService) // 이미 만드신 UserService 등록
                        )
                        .successHandler(oAuth2LoginSuccessHandler) // 위에서 만든 핸들러 등록
                );

        //닉네임 변경검증
        http
                .addFilterBefore(new JWTFilter(), UsernamePasswordAuthenticationFilter.class);

        // 세션 정책을 STATELESS로 설정 (JWT 인증 방식이므로)
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
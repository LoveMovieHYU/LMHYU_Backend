package Recommend.Movie.Config;


import Recommend.Movie.User.Handler.OAuth2LoginSuccessHandler;
import Recommend.Movie.User.Service.CustomOAuthService;
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
    private final CustomOAuthService customOAuthService;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, CustomOAuthService customOAuthService) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.customOAuthService = customOAuthService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 경로별 인가 작업
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/jwt/refresh", "/api/movies/**","/api/home", "/api/search/**").permitAll() // tmdb 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll() // swagger 허용
                        .requestMatchers("/", "/login-test.html", "/test/**").permitAll() // 테스트 경로 허용
                        .requestMatchers("/api/v1/auth/**", "/oauth2/**").permitAll() // 소셜 로그인 허용
                        .requestMatchers("/api/feedback/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                );

        // CSRF, Form Login, HTTP Basic 인증 비활성화
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());


        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfo) -> userInfo
                                .userService(customOAuthService) // 이미 만드신 UserService 등록
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
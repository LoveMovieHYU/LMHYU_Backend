package Recommend.Movie.Config;


import Recommend.Movie.User.Handler.OAuth2LoginSuccessHandler;
import Recommend.Movie.User.Service.CustomOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean devEndpointsEnabled;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          CustomOAuthService customOAuthService,
                          @Value("${app.security.dev-endpoints-enabled:false}") boolean devEndpointsEnabled) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.customOAuthService = customOAuthService;
        this.devEndpointsEnabled = devEndpointsEnabled;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 경로별 인가 작업
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/jwt/refresh", "/api/movies/**", "/api/home", "/api/search/**").permitAll();
                    auth.requestMatchers("/api/v1/auth/**", "/oauth2/**").permitAll();
                    auth.requestMatchers("/api/feedback/**").permitAll();
                    auth.requestMatchers("/login/**").permitAll();
                    if (devEndpointsEnabled) {
                        auth.requestMatchers("/", "/login-test.html", "/test/**", "/swagger-ui/**",
                                "/v3/api-docs/**", "/swagger-resources/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                });

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

        http
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"토큰이 만료되었거나 유효하지 않습니다.\"}");
                            } else {
                                response.sendRedirect("/login");
                            }
                        })
                );
        return http.build();
    }

}

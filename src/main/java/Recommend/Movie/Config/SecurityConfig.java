package Recommend.Movie.Config;


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
                        .requestMatchers("/api/v1/auth/google", "/jwt/refresh", "/api/v1/auth/naver","/api/movies/**").permitAll()
                        // 그 외 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated()
                );

        // 세션 정책을 STATELESS로 설정 (JWT 인증 방식이므로)
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
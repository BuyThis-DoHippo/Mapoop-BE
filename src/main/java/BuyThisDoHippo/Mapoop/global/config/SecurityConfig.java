package BuyThisDoHippo.Mapoop.global.config;

import BuyThisDoHippo.Mapoop.global.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 사용)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안함 (JWT 사용)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // URL별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**",
                                "/error"
                        ).permitAll()

                        // 공개 API (인증 불필요)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/toilets/*").permitAll()
                        .requestMatchers("/api/toilets/emergency").permitAll()
                        .requestMatchers("/api/toilets/{id}/reviews").permitAll()
                        .requestMatchers("/api/toilets/{id}/rating").permitAll()
                        .requestMatchers("/api/toilets/{id}/review-count").permitAll()
                        .requestMatchers("/api/toilets/{id}/top-tags").permitAll()
                        .requestMatchers("/api/reviews/{id}").permitAll()
                        .requestMatchers("/api/users/{id}/reviews").permitAll()
                        .requestMatchers("/api/tags/review").permitAll()
                        .requestMatchers("/api/tags").permitAll()
                        .requestMatchers("/api/chatbot/**").permitAll()
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/api/map/markers").permitAll()

                        // 인증 필요한 API
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/toilets").authenticated()  // 화장실 등록은 인증 필요
                        .requestMatchers(HttpMethod.POST, "/api/toilets/*/reviews").authenticated()  // 리뷰 작성은 인증 필요
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").authenticated()   // 리뷰 수정은 인증 필요
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated() // 리뷰 삭제는 인증 필요
                        .requestMatchers(HttpMethod.PUT, "api/toilets/*").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/toilets/*/images").authenticated()
                        .requestMatchers("/api/toilets/*/images/*").authenticated()

                        // 나머지는 모두 인증 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"
        ));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
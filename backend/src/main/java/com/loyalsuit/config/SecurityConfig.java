package com.loyalsuit.config;

import com.loyalsuit.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                // Authentication endpoints (register / login / password reset) are public.
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password").permitAll()
                // Public storefront endpoints (these resolve tenant from the URL/host,
                // NOT from a JWT principal — see StoreController in Phase 2).
                .requestMatchers(HttpMethod.GET, "/api/v1/store/**").permitAll()
                // Guest carts are anonymous: cart mutations resolve tenant by slug and
                // a client cart token (no DB persistence, prices recomputed server-side).
                .requestMatchers("/api/v1/store/*/cart", "/api/v1/store/*/cart/**").permitAll()
                // Guest cash checkout is anonymous; an authenticated customer (if any) is
                // linked to the order. Totals + stock are validated server-side.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/store/*/checkout").permitAll()
                // Guest return requests are anonymous, verified by order number + email.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/store/*/orders/*/returns").permitAll()
                // NOTE: /api/v1/catalog/** is tenant-admin scoped and stays authenticated.
                // Its controllers dereference principal.getTenantId(), so they must never
                // be public or they NPE on anonymous requests.
                // Webhooks (verified by gateway signature, not JWT)
                .requestMatchers("/api/v1/payments/webhooks/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // LinkedHashSet dedups when frontendUrl already equals the localhost default.
        config.setAllowedOrigins(List.copyOf(new LinkedHashSet<>(
                List.of(frontendUrl, "http://localhost:3000"))));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

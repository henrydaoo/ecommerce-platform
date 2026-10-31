package com.ecommerce.platform.config;

import com.ecommerce.platform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/payment/vnpay/**").permitAll()
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("POST", "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers("GET", "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers("GET", "/api/v1/orders/*")
                                .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("GET", "/api/v1/products/admin").hasRole("ADMIN")
                        .requestMatchers("GET", "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**",
                                "/actuator/prometheus").hasRole("ADMIN")
                                                                                  
                                                                                
                        .requestMatchers("/api/v1/orders/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/orders/*/status").hasRole("ADMIN")
                        .requestMatchers("POST", "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("PUT", "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("DELETE", "/api/v1/products/**", "/api/v1/categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

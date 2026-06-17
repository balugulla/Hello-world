package org.gulla.service.gulla.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final EasyApplyConfigProperties config;

    public SecurityConfig(EasyApplyConfigProperties config) {
        this.config = config;
    }

    @Bean
    @Profile("prod")
    public SecurityFilterChain securityFilterChainProduction(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new ApiKeyAuthenticationFilter(config), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Profile({"dev", "test"})
    public SecurityFilterChain securityFilterChainDevelopment(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    private static class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
        private static final String API_KEY_HEADER = "X-API-Key";
        private final EasyApplyConfigProperties config;

        public ApiKeyAuthenticationFilter(EasyApplyConfigProperties config) {
            this.config = config;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String requestApiKey = request.getHeader(API_KEY_HEADER);
            String configuredApiKey = config.getApi().getKey();

            if (configuredApiKey == null || configuredApiKey.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"API key not configured on server\"}");
                return;
            }

            if (requestApiKey == null || !requestApiKey.equals(configuredApiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
                return;
            }

            filterChain.doFilter(request, response);
        }
    }
}

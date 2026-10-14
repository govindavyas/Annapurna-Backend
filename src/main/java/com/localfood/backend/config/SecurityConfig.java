package com.localfood.backend.config;

import com.localfood.backend.admin.AdminAccountUserDetailsService;
import com.localfood.backend.admin.AdminUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/actuator/health/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/menu/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/orders/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/customers/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/customer/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/features/**"),
                    AntPathRequestMatcher.antMatcher("/media/**")
                ).permitAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/api/v1/admin/me"),
                    AntPathRequestMatcher.antMatcher("/api/v1/admin/delivery/**")
                ).hasAnyRole("ADMIN", "DELIVERY_STAFF")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/admin/categories/**")).hasRole("SUPER_ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/admin/accounts/**"),
                    AntPathRequestMatcher.antMatcher("/api/v1/admin/branches/*/accounts"))
                    .hasRole("SUPER_ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher(org.springframework.http.HttpMethod.GET, "/api/v1/admin/menu/**"))
                    .hasRole("ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/admin/menu/**"))
                    .hasRole("SUPER_ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher(org.springframework.http.HttpMethod.POST, "/api/v1/admin/branches"),
                    AntPathRequestMatcher.antMatcher(org.springframework.http.HttpMethod.DELETE, "/api/v1/admin/branches/*"),
                    AntPathRequestMatcher.antMatcher(org.springframework.http.HttpMethod.PUT, "/api/v1/admin/branches/*"))
                    .hasRole("SUPER_ADMIN")
                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/admin/**")).hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults())
                .addFilterAfter(new AdminTotpHeaderFilter(), BasicAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Admin-Totp",
            "X-Customer-Token",
            "Accept",
            "Origin"
        ));
        cors.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static class AdminTotpHeaderFilter extends OncePerRequestFilter {

        private static final String HEADER = "X-Admin-Totp";

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/api/v1/admin/") || "OPTIONS".equalsIgnoreCase(request.getMethod());
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader("WWW-Authenticate", "Basic realm=\"Admin\"");
                response.getWriter().write("Invalid admin credentials");
                return;
            }

            if (!(authentication.getPrincipal() instanceof AdminUserDetails principal)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Invalid admin principal");
                return;
            }

            String provided = request.getHeader(HEADER);
            if (provided == null || !provided.equals(principal.getTotpSecret())) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("Missing or invalid admin second factor");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }
}

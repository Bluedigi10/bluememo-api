package com.bluedigi.bluememo.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bluedigi.bluememo.config.properties.CorsProperties;
import com.bluedigi.bluememo.shared.exception.SecurityErrorHandler;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final SecurityErrorHandler securityErrorHandler;
        private final CorsProperties corsProperties;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, SecurityErrorHandler securityErrorHandler, CorsProperties corsProperties) {
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
            this.securityErrorHandler = securityErrorHandler;
            this.corsProperties = corsProperties;
        }

        @Bean
        public FilterRegistrationBean<JwtAuthenticationFilter>
        jwtAuthenticationFilterRegistration(
                JwtAuthenticationFilter filter
        ) {
            FilterRegistrationBean<JwtAuthenticationFilter> registration =
                    new FilterRegistrationBean<>(filter);

            registration.setEnabled(false);

            return registration;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(corsProperties.allowedOrigins());
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setAllowCredentials(true);
        
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )

                    .cors(Customizer.withDefaults())
                
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint(securityErrorHandler)
                            .accessDeniedHandler(securityErrorHandler)
                    )
                    .authorizeHttpRequests(auth -> auth
                            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                            .requestMatchers(
                                    "/auth/**",
                                    "/v3/api-docs/**",
                                    "/swagger-ui/**",
                                    "/swagger-ui.html",
                                    "/actuator/health"
                            ).permitAll()
                            .requestMatchers(
                                    HttpMethod.POST, "/webhooks/telegram"
                            ).permitAll()
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(
                            jwtAuthenticationFilter,
                            UsernamePasswordAuthenticationFilter.class
                    )
                    .build();
        }
}

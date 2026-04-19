package com.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtUtil jwtUtil; // ✅ inject properly

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(jwtUtil);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                // ❌ disable csrf (stateless API)
                                .csrf(csrf -> csrf.disable())

                                // ✅ enable cors
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // ✅ stateless session
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // ✅ authorization rules
                                .authorizeHttpRequests(auth -> auth

                                                // PUBLIC
                                                .requestMatchers(
                                                                "/auth/register",
                                                                "/auth/login",
                                                                "/auth/forgot-password",
                                                                "/auth/reset-password",
                                                                "/auth/admin/login",
                                                                "/auth/admin/register")
                                                .permitAll()

                                                // ADMIN ONLY
                                                .requestMatchers("/auth/admin/**").hasAuthority("ADMIN")

                                                // ALL OTHER APIs
                                                .anyRequest().authenticated())

                                // ❌ REMOVE THIS (not needed for JWT)
                                // .httpBasic(Customizer.withDefaults())

                                // ✅ ADD JWT FILTER
                                .addFilterBefore(jwtAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(Arrays.asList(
                                "https://taxinstant.vercel.app",
                                "https://www.taxinstant.com",
                                "http://localhost:5173"));

                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS"));

                configuration.setAllowedHeaders(Arrays.asList("*"));

                configuration.setAllowCredentials(true);

                configuration.setExposedHeaders(Arrays.asList("Authorization"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration("/**", configuration);

                return source;
        }
}
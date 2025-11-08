package com.example.boards.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // CSRF Protection with cookie-based tokens for REST API
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()

            // Authorization configuration
            .authorizeRequests()
                // Public endpoints (no authentication required)
                .antMatchers("/api/users/signup", "/api/users/login").permitAll()
                // All other API endpoints require authentication
                .antMatchers("/api/**").authenticated()
                // Allow all other requests (for development)
                .anyRequest().permitAll()
            .and()

            // Disable HTTP Basic authentication (using session-based auth)
            .httpBasic().disable()

            // Disable default login form (using custom React login)
            .formLogin().disable()

            // Session management configuration
            .sessionManagement()
                .maximumSessions(5) // Allow max 5 concurrent sessions per user
                .maxSessionsPreventsLogin(false); // Invalidate oldest session when limit reached
    }
}

package com.example.demo.config;

import com.example.demo.entities.User;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasRole("OWNER")

                        .requestMatchers("/api/staff/**").hasRole("OWNER")

                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/customers/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/customers/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("STAFF", "OWNER", "CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasAnyRole("STAFF", "OWNER")

                        .requestMatchers(HttpMethod.GET, "/api/warranties/customer").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/warranties/customer/**").hasRole("CUSTOMER")

                        .requestMatchers("/api/suppliers/**").hasRole("OWNER")
                        .requestMatchers("/api/import-receipts/**").hasRole("OWNER")
                        .requestMatchers("/api/warranties/**").hasAnyRole("STAFF", "OWNER")

                        .requestMatchers(HttpMethod.GET, "/api/vouchers", "/api/vouchers/search").hasRole("OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/vouchers").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/vouchers/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/vouchers/**").hasRole("OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/vouchers/**").hasRole("OWNER")

                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**").hasAnyRole("STAFF", "OWNER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                user = userRepository.findByEmail(username);
            }

            User foundUser = user.orElseThrow(() -> new UsernameNotFoundException("User not found"));
            return org.springframework.security.core.userdetails.User
                    .withUsername(foundUser.getUsername())
                    .password(foundUser.getPassword())
                    .disabled(Boolean.FALSE.equals(foundUser.getIsActive()))
                    .roles(foundUser.getRole().name())
                    .build();
        };
    }
}

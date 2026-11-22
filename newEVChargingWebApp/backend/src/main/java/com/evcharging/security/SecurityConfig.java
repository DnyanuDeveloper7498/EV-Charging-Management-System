package com.evcharging.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(csrf -> csrf.disable())              // stateless API, token in header (no cookies)
                .cors(cors -> cors.configurationSource(corsConfigurationSource)).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> auth.requestMatchers("/error").permitAll().requestMatchers("/ws", "/ws/**").permitAll().requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // public
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll().requestMatchers(HttpMethod.POST, "/api/users/register", "/api/users/login", "/api/users/forgot-password", "/api/users/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll() // verified by Razorpay signature
                        .requestMatchers(HttpMethod.GET, "/api/stations", "/api/stations/*", "/api/stations/*/reviews").permitAll()

                        // station management
                        .requestMatchers(HttpMethod.POST, "/api/stations").hasAnyRole("ADMIN", "STATION_OWNER", "STATION_ADMIN")


                        .requestMatchers("/api/stations/*/slots").hasRole("ADMIN")
                        // owner-vs-admin ownership is checked in ChargingStationService
                        .requestMatchers(HttpMethod.PUT, "/api/stations/*").hasAnyRole("ADMIN", "STATION_OWNER", "STATION_ADMIN")


                        .requestMatchers(HttpMethod.DELETE, "/api/stations/*").hasAnyRole("ADMIN", "STATION_OWNER")

                        // admin only
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("ADMIN", "USER_ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/vehicles").hasAnyRole("ADMIN", "VEHICLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/bookings", "/api/bookings/status/*", "/api/bookings/station/*").hasAnyRole("ADMIN", "CHARGING_ADMIN")


                        .requestMatchers(HttpMethod.GET, "/api/sessions/all", "/api/sessions/active").hasAnyRole("ADMIN", "CHARGING_ADMIN")



                        .requestMatchers(HttpMethod.GET, "/api/payments/all", "/api/payments/status/*").hasAnyRole("ADMIN", "FINANCE_ADMIN")

                        // everything else under /api needs a valid login (ownership is checked in the services)
                        .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
                        .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please login to continue."))
                                .accessDeniedHandler((request, response, e) -> writeError(response, HttpServletResponse.SC_FORBIDDEN, "You do not have permission to do this.")))
                        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + message + "\",\"status\":" + status + "}");
    }
}

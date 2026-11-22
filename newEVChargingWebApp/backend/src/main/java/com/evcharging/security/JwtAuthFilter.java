package com.evcharging.security;

import com.evcharging.model.User;
import com.evcharging.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <jwt>", validates it and loads the user from the database so a deleted
 * user or a changed role takes effect immediately. An invalid/missing token simply leaves the request
 * unauthenticated; the security chain then answers 401 for protected endpoints.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                Claims claims = jwtService.parse(token);
                int userId = Integer.parseInt(claims.getSubject());
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    AuthUser principal = new AuthUser(user.getUserId(), user.getEmail(), user.getRole());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}

package com.evcharging.security;


import com.evcharging.model.User;
import com.evcharging.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public WebSocketAuthInterceptor(JwtService jwtService, UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor==null)
        {
            return  message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String header = accessor.getFirstNativeHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                throw new IllegalArgumentException("WebSocket authentication required");
            }

            String token = header.substring(7).trim();

            try {

                Claims claims = jwtService.parse(token);

                int userId = Integer.parseInt(claims.getSubject());

                User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(String.valueOf(user.getUserId()), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));



                accessor.setUser(authentication);



            } catch (JwtException | IllegalArgumentException ex) {

                throw new IllegalArgumentException("Invalid WebSocket JWT token", ex);
            }
        }

        return message;
    }
}

package com.evcharging.config;

import com.evcharging.security.WebSocketAuthInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        config.enableSimpleBroker("/queue");

        config.setApplicationDestinationPrefixes("/app");

        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        String[] origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);

        registry.addEndpoint("/ws").setAllowedOriginPatterns(origins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        registration.interceptors(authInterceptor);
    }
}

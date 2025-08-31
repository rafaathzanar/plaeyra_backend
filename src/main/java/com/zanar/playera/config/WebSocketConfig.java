package com.zanar.playera.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Enable simple message broker for sending messages to clients
    config.enableSimpleBroker("/topic", "/queue");
    // Set prefix for client-to-server messages
    config.setApplicationDestinationPrefixes("/app");
    // Set prefix for user-specific messages
    config.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Register STOMP endpoints for WebSocket connections
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // Allow all origins for development
        .withSockJS(); // Enable SockJS fallback
  }
}

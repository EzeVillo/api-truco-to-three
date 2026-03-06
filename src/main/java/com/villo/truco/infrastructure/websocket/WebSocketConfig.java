package com.villo.truco.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final JwtDecoder jwtDecoder;

  public WebSocketConfig(final JwtDecoder jwtDecoder) {

    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public void configureMessageBroker(final MessageBrokerRegistry registry) {
    // el broker simple maneja los topics donde el server pushea mensajes
    registry.enableSimpleBroker("/topic", "/queue");
    // prefijo para mensajes que van del cliente al servidor (no lo usamos en este MVP)
    registry.setApplicationDestinationPrefixes("/app");
    // destino por usuario/sesión para notificaciones privadas
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void registerStompEndpoints(final StompEndpointRegistry registry) {
    // endpoint nativo — para Postman y clientes WS nativos
    registry.addEndpoint("/ws").setAllowedOriginPatterns("*");

    // endpoint SockJS — para el front si lo necesita
    registry.addEndpoint("/ws-sockjs").setAllowedOriginPatterns("*").withSockJS();
  }

  @Override
  public void configureClientInboundChannel(final ChannelRegistration registration) {

    registration.interceptors(new WebSocketAuthInterceptor(this.jwtDecoder));
  }

}

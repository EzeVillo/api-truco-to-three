package com.villo.truco.infrastructure.websocket;

import com.villo.truco.infrastructure.config.TrucoWebProperties;
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

  private final TrucoWebProperties webProperties;

  public WebSocketConfig(final JwtDecoder jwtDecoder, final TrucoWebProperties webProperties) {

    this.jwtDecoder = jwtDecoder;
    this.webProperties = webProperties;
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

    final var allowedOrigins = this.webProperties.allowedOriginsArray();

    // endpoint nativo — para Postman y clientes WS nativos
    registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);

    // endpoint SockJS — para el front si lo necesita
    registry.addEndpoint("/ws-sockjs").setAllowedOriginPatterns(allowedOrigins).withSockJS();
  }

  @Override
  public void configureClientInboundChannel(final ChannelRegistration registration) {

    registration.interceptors(new WebSocketAuthInterceptor(this.jwtDecoder));
  }

  @Override
  public void configureClientOutboundChannel(final ChannelRegistration registration) {

    registration.taskExecutor().corePoolSize(1).maxPoolSize(1);
  }

}

package com.edtech.platform.communication.config;

import com.edtech.platform.common.config.properties.CorsProperties;
import com.edtech.platform.common.security.HtmlSessionWebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtAuthInterceptor stompJwtAuthInterceptor;
    private final CorsProperties corsProperties;
    private final HtmlSessionWebSocketHandshakeInterceptor htmlSessionHandshakeInterceptor;

    public WebSocketConfig(StompJwtAuthInterceptor stompJwtAuthInterceptor, CorsProperties corsProperties,
                           HtmlSessionWebSocketHandshakeInterceptor htmlSessionHandshakeInterceptor) {
        this.stompJwtAuthInterceptor = stompJwtAuthInterceptor;
        this.corsProperties = corsProperties;
        this.htmlSessionHandshakeInterceptor = htmlSessionHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(htmlSessionHandshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtAuthInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtAuthInterceptor.outboundInterceptor());
    }
}

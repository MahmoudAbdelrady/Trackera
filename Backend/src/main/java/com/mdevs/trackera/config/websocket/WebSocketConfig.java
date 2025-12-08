package com.mdevs.trackera.config.websocket;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.shared.WebSocketChannelInterceptor;
import com.mdevs.trackera.shared.WebSocketJwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketJwtHandshakeInterceptor jwtHandshakeInterceptor;

    private final WebSocketChannelInterceptor channelInterceptor;

    public WebSocketConfig(WebSocketJwtHandshakeInterceptor jwtHandshakeInterceptor, WebSocketChannelInterceptor channelInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.channelInterceptor = channelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(AppConfig.getFrontendUrl())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/trackera");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptor);
    }
}

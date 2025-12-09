package com.mdevs.trackera.config.websocket;

import com.mdevs.trackera.config.general.AppConfig;
import com.mdevs.trackera.shared.WebSocketChannelRuleInterceptor;
import com.mdevs.trackera.shared.WebSocketJwtChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketJwtChannelInterceptor jwtChannelInterceptor;

    private final WebSocketChannelRuleInterceptor channelRuleInterceptor;

    public WebSocketConfig(WebSocketJwtChannelInterceptor jwtChannelInterceptor, WebSocketChannelRuleInterceptor channelRuleInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
        this.channelRuleInterceptor = channelRuleInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOrigins(AppConfig.getFrontendUrl());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/trackera");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor, channelRuleInterceptor);
    }
}

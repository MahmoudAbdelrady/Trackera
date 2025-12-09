package com.mdevs.trackera.shared;

import com.mdevs.trackera.entity.User;
import com.mdevs.trackera.service.UserService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Objects;
import java.util.function.BiPredicate;

@Component
public class WebSocketChannelRuleInterceptor implements ChannelInterceptor {
    private final UserService userService;

    private final WebSocketAuthRegistry webSocketAuthRegistry;

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    public WebSocketChannelRuleInterceptor(UserService userService, WebSocketAuthRegistry webSocketAuthRegistry) {
        this.userService = userService;
        this.webSocketAuthRegistry = webSocketAuthRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String userId = (String) Objects.requireNonNull(accessor.getSessionAttributes()).get("userId");
            User loggedUser = userService.findByUuidOrThrow(userId);

            for (var entry : webSocketAuthRegistry.getRules().entrySet()) {
                String pattern = entry.getKey();
                BiPredicate<User, String> validator = entry.getValue();

                if (pathMatch(pattern, destination)) {
                    validator.test(loggedUser, destination);
                    break;
                }
            }
        }

        return message;
    }

    private boolean pathMatch(String pattern, String destination) {
        return ANT_PATH_MATCHER.match(pattern, destination);
    }
}

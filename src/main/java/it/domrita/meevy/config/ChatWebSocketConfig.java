
package it.domrita.meevy.config;

import it.domrita.meevy.chat.websocket.ChatStompAuthInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@Profile("!dev")
@EnableWebSocketMessageBroker
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompAuthInterceptor authInterceptor;
    private final String allowedOriginPatterns;

    public ChatWebSocketConfig(
            ChatStompAuthInterceptor authInterceptor,
            @Value("${meevy.websocket.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
            String allowedOriginPatterns
    ) {
        this.authInterceptor = authInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket nativo, senza SockJS.
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOriginPatterns.split(","));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Usiamo code personali, non topic pubblici di gruppo.
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }
}

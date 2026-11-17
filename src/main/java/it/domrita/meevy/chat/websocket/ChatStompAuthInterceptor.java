package it.domrita.meevy.chat.websocket;


import lombok.RequiredArgsConstructor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatStompAuthInterceptor implements ChannelInterceptor {

    private static final String NOTIFICATION_DESTINATION =
            "/user/queue/group-message-notifications";

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

        // Le heartbeat STOMP non contengono un comando.
        if (headers == null || headers.getCommand() == null) {
            return message;
        }

        StompCommand command = headers.getCommand();

        if (command == StompCommand.CONNECT) {
            String authorization =
                    headers.getFirstNativeHeader("Authorization");

            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {
                throw new BadCredentialsException(
                        "Token WebSocket mancante."
                );
            }

            String token = authorization
                    .substring("Bearer ".length())
                    .trim();

            if (token.isEmpty()) {
                throw new BadCredentialsException(
                        "Token WebSocket mancante."
                );
            }

            // Stessi controlli di firma, issuer e scadenza delle API REST.
            Jwt jwt = jwtDecoder.decode(token);

            validateUserId(jwt);

            headers.setUser(new JwtAuthenticationToken(jwt));
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            if (!(headers.getUser() instanceof JwtAuthenticationToken auth)) {
                throw new AccessDeniedException(
                        "Connessione non autenticata."
                );
            }

            // Ricontrolliamo la validita' quando il client si iscrive.
            jwtDecoder.decode(auth.getToken().getTokenValue());

            if (!NOTIFICATION_DESTINATION.equals(headers.getDestination())) {
                throw new AccessDeniedException(
                        "Destinazione WebSocket non consentita."
                );
            }

            return message;
        }

        if (command == StompCommand.DISCONNECT
                || command == StompCommand.UNSUBSCRIBE) {
            return message;
        }

        // Il contenuto dei messaggi si invia tramite REST.
        throw new AccessDeniedException(
                "Operazione WebSocket non consentita."
        );
    }

    private void validateUserId(Jwt jwt) {
        try {
            if (Long.parseLong(jwt.getSubject()) <= 0) {
                throw new NumberFormatException("ID non positivo");
            }
        } catch (NumberFormatException | NullPointerException ex) {
            throw new BadCredentialsException(
                    "Identificativo JWT non valido.", ex
            );
        }
    }
}

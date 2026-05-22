package messenger.bereza.web.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messenger.bereza.security.BerezaUserDetails;
import messenger.bereza.service.ChatEventPublisher;
import messenger.bereza.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceListener {

    private final ChatEventPublisher chatEventPublisher;
    private final UserService userService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        var user = extractUser(StompHeaderAccessor.wrap(event.getMessage()).getUser());
        if (user != null) {
            chatEventPublisher.publishPresence(user.getId(), true);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        var user = extractUser(event.getUser());
        if (user != null) {
            userService.touchLastSeen(user.getId());
            chatEventPublisher.publishPresence(user.getId(), false);
        }
    }

    private static BerezaUserDetails extractUser(java.security.Principal p) {
        if (p instanceof Authentication auth && auth.getPrincipal() instanceof BerezaUserDetails u) {
            return u;
        }
        return null;
    }
}

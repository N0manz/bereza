package messenger.bereza.web.ws;

import lombok.RequiredArgsConstructor;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.security.BerezaUserDetails;
import messenger.bereza.service.ChatEventPublisher;
import messenger.bereza.service.MessageService;
import messenger.bereza.web.dto.message.MessageResponse;
import messenger.bereza.web.dto.message.SendMessageRequest;
import messenger.bereza.web.mapper.MessageMapper;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Эндпоинты STOMP. Аутентификация выполняется через HTTP-сессию,
 * которую SockJS прокидывает в WebSocket. principal — экземпляр {@link BerezaUserDetails}.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private final ChatEventPublisher chatEventPublisher;
    private final UserRepository userRepository;

    /** Клиент шлёт STOMP-сообщение в /app/chats/{chatId}/send. */
    @MessageMapping("/chats/{chatId}/send")
    public void send(@DestinationVariable Long chatId,
                     @Payload SendMessageRequest req,
                     @AuthenticationPrincipal BerezaUserDetails principal) {
        var msg = messageService.send(chatId, principal.getId(), req);
        chatEventPublisher.publishMessage(msg);
    }

    /** «{user} печатает…»: клиент шлёт /app/chats/{chatId}/typing — мы транслируем в /topic. */
    @MessageMapping("/chats/{chatId}/typing")
    public void typing(@DestinationVariable Long chatId,
                       @AuthenticationPrincipal BerezaUserDetails principal) {
        chatEventPublisher.publishTyping(chatId, principal.getId(), principal.getDisplayName());
    }

    /** Отметка прочтения через WS. */
    @MessageMapping("/chats/{chatId}/read")
    public void read(@DestinationVariable Long chatId,
                     @Payload Map<String, Object> payload,
                     @AuthenticationPrincipal BerezaUserDetails principal) {
        Long messageId = ((Number) payload.get("messageId")).longValue();
        messageService.markRead(chatId, principal.getId(), messageId);
        chatEventPublisher.publishRead(chatId, messageId, principal.getId());
    }

    /** При подписке на /topic/chats.{id} отдаём 50 последних сообщений в queue клиента (не обязательно). */
    @SubscribeMapping("/chats.{chatId}.history")
    public java.util.List<MessageResponse> history(@DestinationVariable Long chatId,
                                                   @AuthenticationPrincipal BerezaUserDetails principal) {
        var slice = messageService.history(chatId, principal.getId(), null, 50);
        return slice.stream().map(messageMapper::toResponse).toList();
    }
}

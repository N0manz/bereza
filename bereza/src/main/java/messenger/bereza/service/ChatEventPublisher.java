package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Message;
import messenger.bereza.web.mapper.MessageMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Публикует события чата (новое сообщение, прочтение, печатает) подписчикам
 * через SimpMessagingTemplate. Используется и из REST, и из WS-контроллеров,
 * чтобы канал доставки был единым.
 */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private final SimpMessagingTemplate ws;
    private final MessageMapper messageMapper;

    public void publishMessage(Message msg) {
        var payload = messageMapper.toResponse(msg);
        ws.convertAndSend("/topic/chats." + msg.getChat().getId(), payload);
    }

    public void publishRead(Long chatId, Long messageId, Long readerId) {
        ws.convertAndSend("/topic/chats." + chatId + ".reads",
                Map.of("messageId", messageId, "userId", readerId));
    }

    public void publishTyping(Long chatId, Long userId, String displayName) {
        ws.convertAndSend("/topic/chats." + chatId + ".typing",
                Map.of("userId", userId, "displayName", displayName));
    }

    public void publishPresence(Long userId, boolean online) {
        ws.convertAndSend("/topic/presence",
                Map.of("userId", userId, "online", online));
    }
}

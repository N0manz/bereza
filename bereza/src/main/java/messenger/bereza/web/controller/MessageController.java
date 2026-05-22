package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.ChatEventPublisher;
import messenger.bereza.service.MessageService;
import messenger.bereza.web.dto.message.MessageResponse;
import messenger.bereza.web.dto.message.SendMessageRequest;
import messenger.bereza.web.mapper.MessageMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private final ChatEventPublisher chatEventPublisher;
    private final CurrentUserProvider currentUser;

    @GetMapping
    public List<MessageResponse> history(
            @PathVariable Long chatId,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "50") int size) {
        var slice = messageService.history(chatId, currentUser.currentUserId(), before, size);
        return slice.stream().map(messageMapper::toResponse).toList();
    }

    @PostMapping
    public MessageResponse send(@PathVariable Long chatId, @Valid @RequestBody SendMessageRequest req) {
        var msg = messageService.send(chatId, currentUser.currentUserId(), req);
        var dto = messageMapper.toResponse(msg);
        chatEventPublisher.publishMessage(msg);
        return dto;
    }

    @PostMapping("/{messageId}/read")
    public void markRead(@PathVariable Long chatId, @PathVariable Long messageId) {
        messageService.markRead(chatId, currentUser.currentUserId(), messageId);
        chatEventPublisher.publishRead(chatId, messageId, currentUser.currentUserId());
    }

    @DeleteMapping("/{messageId}")
    public MessageResponse delete(@PathVariable Long chatId, @PathVariable Long messageId) {
        var msg = messageService.delete(messageId, currentUser.currentUserId());
        var dto = messageMapper.toResponse(msg);
        chatEventPublisher.publishMessage(msg);
        return dto;
    }
}

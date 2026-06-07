package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.ChatType;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.ChatService;
import messenger.bereza.service.MessageService;
import messenger.bereza.web.dto.PageResponse;
import messenger.bereza.web.dto.chat.AddMemberRequest;
import messenger.bereza.web.dto.chat.ChatMemberView;
import messenger.bereza.web.dto.chat.ChatResponse;
import messenger.bereza.web.dto.chat.CreateChatRequest;
import messenger.bereza.web.mapper.ChatMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;
    private final CurrentUserProvider currentUser;
    private final ChatMapper chatMapper;

    @GetMapping
    public PageResponse<ChatResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long uid = currentUser.currentUserId();
        var chats = chatService.listForUser(uid, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(chats.map(c -> chatMapper.toResponse(c, messageService.unreadCount(c.getId(), uid))));
    }

    @GetMapping("/{id}")
    public ChatResponse get(@PathVariable Long id) {
        Long uid = currentUser.currentUserId();
        var c = chatService.get(id, uid);
        return chatMapper.toResponse(c, messageService.unreadCount(c.getId(), uid));
    }

    @PostMapping
    public ChatResponse create(@Valid @RequestBody CreateChatRequest req) {
        Long uid = currentUser.currentUserId();
        var type = req.type();
        var chat = (type == ChatType.PERSONAL || (type == null && req.memberIds().size() == 1))
                ? chatService.createPersonal(uid, req.memberIds().get(0))
                : chatService.createGroup(uid, req.title(), req.description(), req.memberIds());
        return chatMapper.toResponse(chat, 0);
    }

    @GetMapping("/{id}/members")
    public List<ChatMemberView> listMembers(@PathVariable Long id) {
        return chatService.listMembers(id, currentUser.currentUserId())
                .stream().map(chatMapper::toMemberView).toList();
    }

    @PostMapping("/{id}/members")
    public void addMember(@PathVariable Long id, @Valid @RequestBody AddMemberRequest req) {
        chatService.addMember(id, currentUser.currentUserId(), req.userId());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        chatService.removeMember(id, currentUser.currentUserId(), userId);
    }

    @DeleteMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id) {
        chatService.removeMember(id, currentUser.currentUserId(), currentUser.currentUserId());
    }
}

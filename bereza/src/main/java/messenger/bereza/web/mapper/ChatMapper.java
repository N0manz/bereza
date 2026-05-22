package messenger.bereza.web.mapper;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Chat;
import messenger.bereza.domain.ChatMember;
import messenger.bereza.domain.User;
import messenger.bereza.repository.ChatMemberRepository;
import messenger.bereza.web.dto.chat.ChatMemberView;
import messenger.bereza.web.dto.chat.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMapper {

    private final ChatMemberRepository chatMemberRepository;

    public ChatResponse toResponse(Chat chat, long unreadCount) {
        List<ChatMemberView> members = chatMemberRepository.findAllByChatId(chat.getId()).stream()
                .map(this::toMemberView)
                .toList();
        return new ChatResponse(
                chat.getId(), chat.getType(), chat.getTitle(), chat.getDescription(),
                chat.getAvatarUrl(),
                chat.getOwner() != null ? chat.getOwner().getId() : null,
                members, chat.getUpdatedAt(), unreadCount);
    }

    public ChatMemberView toMemberView(ChatMember m) {
        User u = m.getUser();
        return new ChatMemberView(u.getId(), u.getUsername(), u.getDisplayName(),
                u.getAvatarUrl(), u.getRole(), m.getMemberRole());
    }
}

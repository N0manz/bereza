package messenger.bereza.web.dto.chat;

import messenger.bereza.domain.ChatType;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        Long id,
        ChatType type,
        String title,
        String description,
        String avatarUrl,
        Long ownerId,
        List<ChatMemberView> members,
        Instant updatedAt,
        long unreadCount
) {}

package messenger.bereza.web.dto.chat;

import messenger.bereza.domain.MemberRole;
import messenger.bereza.domain.Role;

public record ChatMemberView(
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        Role role,
        MemberRole memberRole
) {}

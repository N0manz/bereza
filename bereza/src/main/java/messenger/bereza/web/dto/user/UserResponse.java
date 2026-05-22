package messenger.bereza.web.dto.user;

import messenger.bereza.domain.Role;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String email,
        Role role,
        String phone,
        String avatarUrl,
        boolean enabled,
        Instant lastSeenAt
) {}

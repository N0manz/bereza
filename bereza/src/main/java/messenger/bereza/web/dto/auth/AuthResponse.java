package messenger.bereza.web.dto.auth;

import messenger.bereza.domain.Role;

public record AuthResponse(
        Long id,
        String username,
        String displayName,
        String email,
        Role role,
        String avatarUrl
) {}

package messenger.bereza.web.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        @Size(max = 32)  String phone,
        @Size(max = 512) String avatarUrl
) {}

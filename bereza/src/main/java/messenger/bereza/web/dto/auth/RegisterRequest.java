package messenger.bereza.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import messenger.bereza.domain.Role;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                message = "username: 3-50 символов, латиница/цифры/._-")
        String username,

        @NotBlank @Email @Size(max = 254) String email,

        @NotBlank @Size(min = 8, max = 100,
                message = "password: 8-100 символов")
        String password,

        @NotBlank @Size(max = 100) String displayName,

        Role role,                       // null => TOURIST
        @Size(max = 32) String phone
) {}

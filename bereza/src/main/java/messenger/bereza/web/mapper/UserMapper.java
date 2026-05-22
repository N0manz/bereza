package messenger.bereza.web.mapper;

import messenger.bereza.domain.User;
import messenger.bereza.web.dto.auth.AuthResponse;
import messenger.bereza.web.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getDisplayName(), u.getEmail(),
                u.getRole(), u.getPhone(), u.getAvatarUrl(), u.isEnabled(), u.getLastSeenAt());
    }

    public AuthResponse toAuth(User u) {
        return new AuthResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getEmail(),
                u.getRole(), u.getAvatarUrl());
    }
}

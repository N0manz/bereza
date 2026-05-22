package messenger.bereza.security;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.User;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public BerezaUserDetails currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof BerezaUserDetails p)) {
            throw new ForbiddenException("Требуется аутентификация");
        }
        return p;
    }

    public Long currentUserId() {
        return currentPrincipal().getId();
    }

    public User currentUser() {
        return userRepository.findById(currentUserId())
                .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
    }
}

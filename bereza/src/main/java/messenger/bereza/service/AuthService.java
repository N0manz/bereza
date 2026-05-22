package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import messenger.bereza.domain.Role;
import messenger.bereza.domain.User;
import messenger.bereza.exception.ConflictException;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.auth.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new ConflictException("Логин уже занят");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email уже используется");
        }

        Role role = req.role() != null ? req.role() : Role.TOURIST;
        if (role == Role.ADMIN) {
            // Регистрация админов запрещена через публичный API
            throw new ConflictException("Невозможно зарегистрировать пользователя с ролью ADMIN");
        }

        User u = User.builder()
                .username(req.username().trim())
                .email(req.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName().trim())
                .role(role)
                .phone(req.phone())
                .enabled(true)
                .locked(false)
                .build();
        User saved = userRepository.save(u);
        log.info("Зарегистрирован пользователь id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }
}

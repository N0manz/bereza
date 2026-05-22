package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.User;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.user.UpdateUserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + id));
    }

    @Transactional(readOnly = true)
    public Page<User> search(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.search(q.trim(), pageable);
    }

    @Transactional
    public User update(Long id, UpdateUserRequest req) {
        User u = get(id);
        if (req.displayName() != null) u.setDisplayName(req.displayName());
        if (req.phone() != null)       u.setPhone(req.phone());
        if (req.avatarUrl() != null)   u.setAvatarUrl(req.avatarUrl());
        return u;
    }

    @Transactional
    public void touchLastSeen(Long id) {
        userRepository.findById(id).ifPresent(u -> u.setLastSeenAt(Instant.now()));
    }
}

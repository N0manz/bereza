package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Notification;
import messenger.bereza.domain.User;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.NotificationRepository;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.notification.NotificationResponse;
import messenger.bereza.web.mapper.NotificationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;
    private final SimpMessagingTemplate ws;

    @Transactional
    public Notification push(Long userId, String type, String title, String body, Map<String, Object> payload) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Notification n = Notification.builder()
                .user(u).type(type).title(title).body(body).payload(payload).read(false)
                .build();
        Notification saved = notificationRepository.save(n);
        NotificationResponse dto = mapper.toResponse(saved);
        // Отправляем индивидуально пользователю
        ws.convertAndSendToUser(u.getUsername(), "/queue/notifications", dto);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long requesterId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Уведомление не найдено"));
        if (!n.getUser().getId().equals(requesterId)) {
            throw new ForbiddenException("Чужое уведомление");
        }
        n.setRead(true);
    }
}

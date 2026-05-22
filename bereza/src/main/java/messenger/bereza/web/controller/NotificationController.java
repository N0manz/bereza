package messenger.bereza.web.controller;

import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.NotificationService;
import messenger.bereza.web.dto.PageResponse;
import messenger.bereza.web.dto.notification.NotificationResponse;
import messenger.bereza.web.mapper.NotificationMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper mapper;
    private final CurrentUserProvider currentUser;

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        var p = notificationService.list(currentUser.currentUserId(),
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(p.map(mapper::toResponse));
    }

    @GetMapping("/unread/count")
    public Map<String, Long> unreadCount() {
        return Map.of("count", notificationService.unreadCount(currentUser.currentUserId()));
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id, currentUser.currentUserId());
    }
}

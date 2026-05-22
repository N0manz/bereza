package messenger.bereza.web.dto.notification;

import java.time.Instant;
import java.util.Map;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        Map<String, Object> payload,
        boolean read,
        Instant createdAt
) {}

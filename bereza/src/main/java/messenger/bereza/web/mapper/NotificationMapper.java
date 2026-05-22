package messenger.bereza.web.mapper;

import messenger.bereza.domain.Notification;
import messenger.bereza.web.dto.notification.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getPayload(), n.isRead(), n.getCreatedAt());
    }
}

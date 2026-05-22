package messenger.bereza.web.dto.message;

import jakarta.validation.constraints.Size;
import messenger.bereza.domain.MessageType;

import java.util.List;
import java.util.Map;

public record SendMessageRequest(
        MessageType type,                  // null => TEXT
        @Size(max = 8000) String content,
        Map<String, Object> payload,
        Long replyToId,
        List<Long> attachmentIds
) {}

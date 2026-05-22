package messenger.bereza.web.dto.message;

import messenger.bereza.domain.MessageType;
import messenger.bereza.web.dto.file.AttachmentView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MessageResponse(
        Long id,
        Long chatId,
        Long senderId,
        String senderDisplayName,
        MessageType type,
        String content,
        Map<String, Object> payload,
        Long replyToId,
        List<AttachmentView> attachments,
        Instant createdAt,
        Instant editedAt,
        boolean deleted,
        List<Long> readBy
) {}

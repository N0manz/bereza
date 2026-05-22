package messenger.bereza.web.mapper;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Attachment;
import messenger.bereza.domain.Message;
import messenger.bereza.repository.AttachmentRepository;
import messenger.bereza.repository.MessageReadRepository;
import messenger.bereza.web.dto.file.AttachmentView;
import messenger.bereza.web.dto.message.MessageResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final AttachmentRepository attachmentRepository;
    private final MessageReadRepository messageReadRepository;

    public MessageResponse toResponse(Message m) {
        List<AttachmentView> attachments = attachmentRepository.findAllByMessageId(m.getId()).stream()
                .map(this::toAttachmentView).toList();
        List<Long> readBy = m.getId() == null
                ? Collections.emptyList()
                : messageReadRepository.findReaderIdsForMessage(m.getId());

        return new MessageResponse(
                m.getId(),
                m.getChat().getId(),
                m.getSender() != null ? m.getSender().getId() : null,
                m.getSender() != null ? m.getSender().getDisplayName() : null,
                m.getType(),
                m.getContent(),
                m.getPayload(),
                m.getReplyTo() != null ? m.getReplyTo().getId() : null,
                attachments,
                m.getCreatedAt(),
                m.getEditedAt(),
                m.getDeletedAt() != null,
                readBy
        );
    }

    public AttachmentView toAttachmentView(Attachment a) {
        return new AttachmentView(a.getId(), a.getOriginalName(), a.getMimeType(),
                a.getSizeBytes(), a.getCategory(),
                "/api/files/" + a.getId());
    }
}

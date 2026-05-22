package messenger.bereza.web.dto.file;

import messenger.bereza.domain.AttachmentCategory;

public record AttachmentView(
        Long id,
        String originalName,
        String mimeType,
        long sizeBytes,
        AttachmentCategory category,
        String downloadUrl
) {}

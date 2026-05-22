package messenger.bereza.web.dto.chat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import messenger.bereza.domain.ChatType;

import java.util.List;

public record CreateChatRequest(
        ChatType type,                // null => GROUP если >2 участников, PERSONAL если 2
        @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotEmpty List<Long> memberIds
) {}

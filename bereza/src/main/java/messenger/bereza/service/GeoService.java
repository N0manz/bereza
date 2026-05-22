package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Chat;
import messenger.bereza.domain.GeoPoint;
import messenger.bereza.domain.User;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.ChatRepository;
import messenger.bereza.repository.GeoPointRepository;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.geo.CreateGeoPointRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoService {

    private final GeoPointRepository geoPointRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Transactional
    public GeoPoint create(Long authorId, CreateGeoPointRequest req) {
        Chat chat = null;
        if (req.chatId() != null) {
            chat = chatRepository.findById(req.chatId())
                    .orElseThrow(() -> new NotFoundException("Чат не найден"));
            chatService.requireMember(chat.getId(), authorId);
        }
        User author = userRepository.findById(authorId).orElseThrow();

        GeoPoint gp = GeoPoint.builder()
                .chat(chat).author(author)
                .type(req.type()).title(req.title()).description(req.description())
                .latitude(req.latitude()).longitude(req.longitude())
                .expiresAt(req.expiresAt())
                .build();
        return geoPointRepository.save(gp);
    }

    @Transactional(readOnly = true)
    public List<GeoPoint> activeInChat(Long chatId, Long requesterId) {
        chatService.requireMember(chatId, requesterId);
        return geoPointRepository.findActiveByChat(chatId, Instant.now());
    }
}

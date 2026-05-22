package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.*;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.*;
import messenger.bereza.web.dto.message.SendMessageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final MessageReadRepository messageReadRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatService chatService;

    @Transactional
    public Message send(Long chatId, Long senderId, SendMessageRequest req) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден"));
        chatService.requireMember(chatId, senderId);

        MessageType type = req.type() != null ? req.type() : MessageType.TEXT;
        if (type == MessageType.TEXT && (req.content() == null || req.content().isBlank())) {
            throw new BadRequestException("Текст сообщения пуст");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Message msg = Message.builder()
                .chat(chat)
                .sender(sender)
                .type(type)
                .content(req.content())
                .payload(req.payload())
                .replyTo(req.replyToId() != null
                        ? messageRepository.findById(req.replyToId())
                            .orElseThrow(() -> new NotFoundException("Цитируемое сообщение не найдено"))
                        : null)
                .build();
        Message saved = messageRepository.save(msg);

        // привязать attachments к сообщению
        List<Long> ids = req.attachmentIds() == null ? Collections.emptyList() : req.attachmentIds();
        for (Long aid : ids) {
            Attachment a = attachmentRepository.findById(aid)
                    .orElseThrow(() -> new NotFoundException("Файл не найден: " + aid));
            if (!a.getUploader().getId().equals(senderId)) {
                throw new ForbiddenException("Файл загружен другим пользователем");
            }
            if (a.getMessage() != null && !a.getMessage().getId().equals(saved.getId())) {
                throw new BadRequestException("Файл уже привязан к другому сообщению");
            }
            a.setMessage(saved);
        }

        chat.setUpdatedAt(Instant.now());
        return saved;
    }

    @Transactional
    public Slice<Message> history(Long chatId, Long requesterId, Long beforeId, int size) {
        chatService.requireMember(chatId, requesterId);
        var pageable = PageRequest.of(0, Math.min(Math.max(size, 1), 100));
        if (beforeId == null) {
            return messageRepository.findRecentByChat(chatId, pageable);
        }
        return messageRepository.findBefore(chatId, beforeId, pageable);
    }

    @Transactional
    public void markRead(Long chatId, Long userId, Long upToMessageId) {
        chatService.requireMember(chatId, userId);

        ChatMember m = chatMemberRepository.find(chatId, userId)
                .orElseThrow(() -> new ForbiddenException("Нет доступа"));
        if (m.getLastReadMessageId() == null || m.getLastReadMessageId() < upToMessageId) {
            m.setLastReadMessageId(upToMessageId);
        }

        // лёгкий вариант: добавляем единичный read receipt для последнего сообщения
        if (!messageReadRepository.existsByIdMessageIdAndIdUserId(upToMessageId, userId)) {
            Message msg = messageRepository.findById(upToMessageId)
                    .orElseThrow(() -> new NotFoundException("Сообщение не найдено"));
            User user = userRepository.getReferenceById(userId);
            messageReadRepository.save(MessageRead.builder()
                    .id(new MessageReadId(upToMessageId, userId))
                    .message(msg).user(user).readAt(Instant.now())
                    .build());
        }
    }

    @Transactional
    public Message delete(Long messageId, Long requesterId) {
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Сообщение не найдено"));
        if (msg.getSender() == null || !msg.getSender().getId().equals(requesterId)) {
            throw new ForbiddenException("Удалять можно только свои сообщения");
        }
        msg.setDeletedAt(Instant.now());
        msg.setContent(null);
        return msg;
    }

    @Transactional
    public long unreadCount(Long chatId, Long userId) {
        ChatMember m = chatMemberRepository.find(chatId, userId).orElse(null);
        if (m == null) return 0;
        Long lastRead = m.getLastReadMessageId();
        return messageRepository.findRecentByChat(chatId, PageRequest.of(0, 100))
                .stream()
                .filter(msg -> lastRead == null || msg.getId() > lastRead)
                .filter(msg -> msg.getSender() == null || !msg.getSender().getId().equals(userId))
                .count();
    }
}

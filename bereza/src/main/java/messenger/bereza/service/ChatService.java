package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.*;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.ChatMemberRepository;
import messenger.bereza.repository.ChatRepository;
import messenger.bereza.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Chat> listForUser(Long userId, Pageable pageable) {
        return chatRepository.findAllForUser(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Chat get(Long chatId, Long requesterId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден: " + chatId));
        requireMember(chatId, requesterId);
        return chat;
    }

    @Transactional
    public Chat createPersonal(Long requesterId, Long peerId) {
        if (requesterId.equals(peerId)) throw new BadRequestException("Нельзя создать чат с собой");
        User a = userRepository.findById(requesterId).orElseThrow();
        User b = userRepository.findById(peerId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + peerId));

        return chatRepository.findPersonalBetween(a.getId(), b.getId(), ChatType.PERSONAL)
                .orElseGet(() -> {
                    Chat chat = chatRepository.save(Chat.builder()
                            .type(ChatType.PERSONAL)
                            .title(null)
                            .owner(a)
                            .build());
                    addMemberInternal(chat, a, MemberRole.MEMBER);
                    addMemberInternal(chat, b, MemberRole.MEMBER);
                    return chat;
                });
    }

    @Transactional
    public Chat createGroup(Long requesterId, String title, String description, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new BadRequestException("Не указаны участники группы");
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("У группы должно быть название");
        }
        User owner = userRepository.findById(requesterId).orElseThrow();
        if (owner.getRole() != Role.GUIDE && owner.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Создавать групповые чаты могут только гиды");
        }

        Chat chat = chatRepository.save(Chat.builder()
                .type(ChatType.GROUP)
                .title(title.trim())
                .description(description)
                .owner(owner)
                .build());

        Set<Long> uniq = new HashSet<>(memberIds);
        uniq.add(requesterId);
        for (Long uid : uniq) {
            User u = userRepository.findById(uid)
                    .orElseThrow(() -> new NotFoundException("Пользователь не найден: " + uid));
            MemberRole r = uid.equals(requesterId) ? MemberRole.OWNER : MemberRole.MEMBER;
            addMemberInternal(chat, u, r);
        }
        return chat;
    }

    @Transactional
    public void addMember(Long chatId, Long requesterId, Long newMemberId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден"));
        if (chat.getType() == ChatType.PERSONAL) {
            throw new BadRequestException("Нельзя добавить участников в личный чат");
        }
        requireOwnerOrAdmin(chatId, requesterId);

        if (chatMemberRepository.existsByChatIdAndUserId(chatId, newMemberId)) {
            return; // идемпотентно
        }
        User u = userRepository.findById(newMemberId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        addMemberInternal(chat, u, MemberRole.MEMBER);
    }

    @Transactional
    public void removeMember(Long chatId, Long requesterId, Long targetUserId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Чат не найден"));
        if (chat.getType() == ChatType.PERSONAL) {
            throw new BadRequestException("Нельзя удалить участников из личного чата");
        }
        if (!requesterId.equals(targetUserId)) {
            // удаление другого участника — только владелец/админ
            requireOwnerOrAdmin(chatId, requesterId);
        }
        ChatMember member = chatMemberRepository.find(chatId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Участник не найден"));
        if (member.getMemberRole() == MemberRole.OWNER) {
            throw new ForbiddenException("Нельзя удалить владельца группы");
        }
        chatMemberRepository.delete(member);
    }

    @Transactional
    public void touch(Long chatId) {
        chatRepository.findById(chatId).ifPresent(c -> c.setUpdatedAt(Instant.now()));
    }

    public void requireMember(Long chatId, Long userId) {
        if (!chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new ForbiddenException("Нет доступа к чату");
        }
    }

    public void requireOwnerOrAdmin(Long chatId, Long userId) {
        ChatMember m = chatMemberRepository.find(chatId, userId)
                .orElseThrow(() -> new ForbiddenException("Нет доступа к чату"));
        if (m.getMemberRole() != MemberRole.OWNER && m.getMemberRole() != MemberRole.ADMIN) {
            throw new ForbiddenException("Действие доступно только админам группы");
        }
    }

    private void addMemberInternal(Chat chat, User user, MemberRole role) {
        ChatMember m = ChatMember.builder()
                .id(new ChatMemberId(chat.getId(), user.getId()))
                .chat(chat).user(user).memberRole(role).joinedAt(Instant.now())
                .build();
        chatMemberRepository.save(m);
    }
}

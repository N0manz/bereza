package messenger.bereza.service;

import messenger.bereza.domain.*;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.repository.ChatMemberRepository;
import messenger.bereza.repository.ChatRepository;
import messenger.bereza.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatRepository chatRepository;
    @Mock ChatMemberRepository chatMemberRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    ChatService chatService;

    // ── helpers ───────────────────────────────────────────────────────────

    private User user(Long id, Role role) {
        return User.builder().id(id).displayName("User" + id).role(role).build();
    }

    // ── tests ─────────────────────────────────────────────────────────────

    @Test
    void createPersonal_throwsBadRequest_whenSelf() {
        assertThatThrownBy(() -> chatService.createPersonal(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("собой");
    }

    @Test
    void createGroup_throwsForbidden_whenUserIsNotGuide() {
        User tourist = user(1L, Role.TOURIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(tourist));

        assertThatThrownBy(() -> chatService.createGroup(1L, "Поход на Байкал", null, List.of(2L)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("гиды");
    }

    @Test
    void createGroup_throwsBadRequest_whenTitleIsBlank() {
        assertThatThrownBy(() -> chatService.createGroup(1L, "   ", null, List.of(2L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("название");
    }

    @Test
    void createGroup_throwsBadRequest_whenMemberListIsEmpty() {
        assertThatThrownBy(() -> chatService.createGroup(1L, "Тур", null, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("участники");
    }

    @Test
    void removeMember_throwsForbidden_whenRemovingOwner() {
        User owner = user(1L, Role.GUIDE);
        Chat group = Chat.builder().id(10L).type(ChatType.GROUP).owner(owner).build();

        ChatMember requesterMember = ChatMember.builder()
                .id(new ChatMemberId(10L, 2L))
                .chat(group).user(user(2L, Role.GUIDE)).memberRole(MemberRole.ADMIN)
                .build();
        ChatMember ownerMember = ChatMember.builder()
                .id(new ChatMemberId(10L, 1L))
                .chat(group).user(owner).memberRole(MemberRole.OWNER)
                .build();

        when(chatRepository.findById(10L)).thenReturn(Optional.of(group));
        when(chatMemberRepository.find(10L, 2L)).thenReturn(Optional.of(requesterMember));
        when(chatMemberRepository.find(10L, 1L)).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> chatService.removeMember(10L, 2L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("владельца");
    }

    @Test
    void removeMember_throwsForbidden_whenPersonalChat() {
        User a = user(1L, Role.TOURIST);
        Chat personal = Chat.builder().id(20L).type(ChatType.PERSONAL).owner(a).build();

        when(chatRepository.findById(20L)).thenReturn(Optional.of(personal));

        assertThatThrownBy(() -> chatService.removeMember(20L, 1L, 2L))
                .isInstanceOf(BadRequestException.class);
    }
}

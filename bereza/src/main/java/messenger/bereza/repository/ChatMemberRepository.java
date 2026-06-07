package messenger.bereza.repository;

import messenger.bereza.domain.ChatMember;
import messenger.bereza.domain.ChatMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {

    @Query("SELECT m FROM ChatMember m JOIN FETCH m.user WHERE m.chat.id = :chatId")
    List<ChatMember> findAllByChatId(@Param("chatId") Long chatId);

    @Query("SELECT m FROM ChatMember m JOIN FETCH m.user WHERE m.chat.id = :chatId AND m.user.id = :userId")
    Optional<ChatMember> find(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Query("SELECT count(m) FROM ChatMember m WHERE m.chat.id = :chatId")
    long countByChatId(@Param("chatId") Long chatId);

    @Query("SELECT m.user.id FROM ChatMember m WHERE m.chat.id = :chatId")
    List<Long> findUserIdsByChatId(@Param("chatId") Long chatId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);
}

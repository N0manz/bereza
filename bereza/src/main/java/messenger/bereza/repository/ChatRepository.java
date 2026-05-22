package messenger.bereza.repository;

import messenger.bereza.domain.Chat;
import messenger.bereza.domain.ChatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("""
           SELECT c FROM Chat c
           JOIN ChatMember m ON m.chat = c
           WHERE m.user.id = :userId
           ORDER BY c.updatedAt DESC
           """)
    Page<Chat> findAllForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
           SELECT c FROM Chat c
           WHERE c.type = :type
             AND EXISTS (SELECT 1 FROM ChatMember m1 WHERE m1.chat = c AND m1.user.id = :a)
             AND EXISTS (SELECT 1 FROM ChatMember m2 WHERE m2.chat = c AND m2.user.id = :b)
             AND (SELECT COUNT(m3) FROM ChatMember m3 WHERE m3.chat = c) = 2
           """)
    Optional<Chat> findPersonalBetween(@Param("a") Long a, @Param("b") Long b, @Param("type") ChatType type);
}

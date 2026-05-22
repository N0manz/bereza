package messenger.bereza.repository;

import messenger.bereza.domain.Message;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
           SELECT m FROM Message m
           LEFT JOIN FETCH m.sender s
           WHERE m.chat.id = :chatId
             AND m.deletedAt IS NULL
           ORDER BY m.id DESC
           """)
    Slice<Message> findRecentByChat(@Param("chatId") Long chatId, Pageable pageable);

    @Query("""
           SELECT m FROM Message m
           LEFT JOIN FETCH m.sender s
           WHERE m.chat.id = :chatId
             AND m.id < :beforeId
             AND m.deletedAt IS NULL
           ORDER BY m.id DESC
           """)
    Slice<Message> findBefore(@Param("chatId") Long chatId,
                              @Param("beforeId") Long beforeId,
                              Pageable pageable);
}

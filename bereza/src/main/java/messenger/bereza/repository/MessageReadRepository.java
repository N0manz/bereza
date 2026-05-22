package messenger.bereza.repository;

import messenger.bereza.domain.MessageRead;
import messenger.bereza.domain.MessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {

    @Query("SELECT mr.user.id FROM MessageRead mr WHERE mr.message.id = :messageId")
    List<Long> findReaderIdsForMessage(@Param("messageId") Long messageId);

    boolean existsByIdMessageIdAndIdUserId(Long messageId, Long userId);
}

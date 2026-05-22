package messenger.bereza.repository;

import messenger.bereza.domain.GeoPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface GeoPointRepository extends JpaRepository<GeoPoint, Long> {

    @Query("""
           SELECT g FROM GeoPoint g
           WHERE g.chat.id = :chatId
             AND (g.expiresAt IS NULL OR g.expiresAt > :now)
           ORDER BY g.createdAt DESC
           """)
    List<GeoPoint> findActiveByChat(@Param("chatId") Long chatId, @Param("now") Instant now);
}

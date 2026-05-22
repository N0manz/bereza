package messenger.bereza.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Раз в час удаляем устаревшие USER_LOCATION-метки. Простая фоновая чистка.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeoPointCleanupJob {

    private final JdbcTemplate jdbc;

    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void cleanup() {
        int deleted = jdbc.update(
                "DELETE FROM geo_points WHERE expires_at IS NOT NULL AND expires_at < ?",
                Instant.now());
        if (deleted > 0) log.info("Удалено устаревших геометок: {}", deleted);
    }
}

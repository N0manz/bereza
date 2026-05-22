package messenger.bereza.web.dto.geo;

import messenger.bereza.domain.GeoPointType;

import java.time.Instant;

public record GeoPointResponse(
        Long id,
        Long chatId,
        Long authorId,
        String authorDisplayName,
        GeoPointType type,
        String title,
        String description,
        double latitude,
        double longitude,
        Instant expiresAt,
        Instant createdAt
) {}

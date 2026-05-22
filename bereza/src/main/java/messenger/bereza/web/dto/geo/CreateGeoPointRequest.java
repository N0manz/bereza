package messenger.bereza.web.dto.geo;

import jakarta.validation.constraints.*;
import messenger.bereza.domain.GeoPointType;

import java.time.Instant;

public record CreateGeoPointRequest(
        Long chatId,
        @NotNull GeoPointType type,
        @Size(max = 150) String title,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("-90")  @DecimalMax("90")  Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        Instant expiresAt
) {}

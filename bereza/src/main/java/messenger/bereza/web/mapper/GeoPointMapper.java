package messenger.bereza.web.mapper;

import messenger.bereza.domain.GeoPoint;
import messenger.bereza.web.dto.geo.GeoPointResponse;
import org.springframework.stereotype.Component;

@Component
public class GeoPointMapper {

    public GeoPointResponse toResponse(GeoPoint g) {
        return new GeoPointResponse(
                g.getId(),
                g.getChat() != null ? g.getChat().getId() : null,
                g.getAuthor().getId(),
                g.getAuthor().getDisplayName(),
                g.getType(), g.getTitle(), g.getDescription(),
                g.getLatitude(), g.getLongitude(),
                g.getExpiresAt(), g.getCreatedAt());
    }
}

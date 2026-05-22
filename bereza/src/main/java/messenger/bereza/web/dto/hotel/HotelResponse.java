package messenger.bereza.web.dto.hotel;

import java.math.BigDecimal;
import java.util.List;

public record HotelResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String city,
        String address,
        Double latitude,
        Double longitude,
        Short stars,
        BigDecimal pricePerNight,
        String currency,
        int roomsAvailable,
        List<String> photos,
        List<String> amenities,
        boolean active
) {}

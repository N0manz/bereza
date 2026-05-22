package messenger.bereza.web.mapper;

import messenger.bereza.domain.Hotel;
import messenger.bereza.web.dto.hotel.HotelResponse;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public HotelResponse toResponse(Hotel h) {
        return new HotelResponse(
                h.getId(),
                h.getOwner() != null ? h.getOwner().getId() : null,
                h.getName(), h.getDescription(), h.getCity(), h.getAddress(),
                h.getLatitude(), h.getLongitude(), h.getStars(),
                h.getPricePerNight(), h.getCurrency(), h.getRoomsAvailable(),
                h.getPhotos(), h.getAmenities(), h.isActive());
    }
}

package messenger.bereza.web.mapper;

import messenger.bereza.domain.Booking;
import messenger.bereza.web.dto.booking.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getHotel().getId(), b.getHotel().getName(),
                b.getUser().getId(),  b.getUser().getDisplayName(),
                b.getCheckIn(), b.getCheckOut(), b.getGuests(), b.getRooms(),
                b.getTotalPrice(), b.getCurrency(), b.getStatus(),
                b.getChat() != null ? b.getChat().getId() : null,
                b.getCreatedAt());
    }
}

package messenger.bereza.web.dto.booking;

import jakarta.validation.constraints.NotNull;
import messenger.bereza.domain.BookingStatus;

public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {}

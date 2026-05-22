package messenger.bereza.web.dto.booking;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull Long hotelId,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull @Future LocalDate checkOut,
        @Min(1) @Max(20) short guests,
        @Min(1) @Max(10) short rooms
) {}

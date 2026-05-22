package messenger.bereza.web.dto.booking;

import messenger.bereza.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long hotelId,
        String hotelName,
        Long userId,
        String userDisplayName,
        LocalDate checkIn,
        LocalDate checkOut,
        short guests,
        short rooms,
        BigDecimal totalPrice,
        String currency,
        BookingStatus status,
        Long chatId,
        Instant createdAt
) {}

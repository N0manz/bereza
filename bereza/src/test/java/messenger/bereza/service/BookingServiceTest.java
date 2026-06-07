package messenger.bereza.service;

import messenger.bereza.domain.*;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.BookingRepository;
import messenger.bereza.repository.HotelRepository;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.booking.CreateBookingRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock HotelRepository hotelRepository;
    @Mock UserRepository userRepository;
    @Mock ChatService chatService;
    @Mock NotificationService notificationService;
    @Mock MessageService messageService;

    @InjectMocks
    BookingService bookingService;

    // ── helpers ───────────────────────────────────────────────────────────

    private Hotel activeHotel(int rooms, BigDecimal price) {
        return Hotel.builder()
                .id(1L)
                .name("Берёза Инн")
                .pricePerNight(price)
                .roomsAvailable(rooms)
                .active(true)
                .build();
    }

    private User tourist() {
        return User.builder().id(10L).displayName("Иван").role(Role.TOURIST).build();
    }

    // ── tests ─────────────────────────────────────────────────────────────

    @Test
    void create_throwsBadRequest_whenHotelInactive() {
        Hotel hotel = activeHotel(5, BigDecimal.valueOf(1000));
        hotel.setActive(false);
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        CreateBookingRequest req = new CreateBookingRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                (short) 1, (short) 1);

        assertThatThrownBy(() -> bookingService.create(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("недоступен");
    }

    @Test
    void create_throwsBadRequest_whenCheckOutNotAfterCheckIn() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(activeHotel(5, BigDecimal.valueOf(1000))));

        LocalDate same = LocalDate.now().plusDays(2);
        CreateBookingRequest req = new CreateBookingRequest(1L, same, same, (short) 1, (short) 1);

        assertThatThrownBy(() -> bookingService.create(10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("выезда");
    }

    @Test
    void create_throwsBadRequest_whenCheckOutBeforeCheckIn() {
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(activeHotel(5, BigDecimal.valueOf(1000))));

        LocalDate checkIn  = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(2);
        CreateBookingRequest req = new CreateBookingRequest(1L, checkIn, checkOut, (short) 1, (short) 1);

        assertThatThrownBy(() -> bookingService.create(10L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_calculatesTotalPriceCorrectly() {
        // 3 ночи × 2000 руб × 2 номера = 12 000 руб
        Hotel hotel = activeHotel(10, BigDecimal.valueOf(2000));
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(bookingRepository.countOverlappingForUser(any(), any(), any(), any())).thenReturn(0L);
        when(bookingRepository.countOverlapping(any(), any(), any())).thenReturn(0L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(tourist()));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });

        CreateBookingRequest req = new CreateBookingRequest(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 4),
                (short) 2, (short) 2);

        Booking saved = bookingService.create(10L, req);

        assertThat(saved.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(12_000));
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void changeStatus_throwsForbidden_whenTouristTriesToConfirm() {
        User tourist = tourist();
        Hotel hotel = activeHotel(5, BigDecimal.valueOf(1000));

        Booking booking = Booking.builder()
                .id(5L)
                .user(tourist)
                .hotel(hotel)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .status(BookingStatus.PENDING)
                .build();

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(tourist.getId())).thenReturn(Optional.of(tourist));

        assertThatThrownBy(() -> bookingService.changeStatus(5L, tourist.getId(), BookingStatus.CONFIRMED))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Турист");
    }

    @Test
    void get_throwsNotFound_whenBookingMissing() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.get(999L, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}

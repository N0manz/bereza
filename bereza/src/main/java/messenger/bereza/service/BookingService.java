package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.*;
import messenger.bereza.exception.BadRequestException;
import messenger.bereza.exception.ConflictException;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.BookingRepository;
import messenger.bereza.repository.HotelRepository;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.booking.CreateBookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;
    private final MessageService messageService;

    @Transactional
    public Booking create(Long userId, CreateBookingRequest req) {
        Hotel hotel = hotelRepository.findById(req.hotelId())
                .orElseThrow(() -> new NotFoundException("Отель не найден"));
        if (!hotel.isActive()) {
            throw new BadRequestException("Отель временно недоступен");
        }
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw new BadRequestException("Дата выезда должна быть позже даты заезда");
        }
        long userOverlap = bookingRepository.countOverlappingForUser(
                hotel.getId(), userId, req.checkIn(), req.checkOut());
        if (userOverlap > 0) {
            throw new ConflictException("У вас уже есть бронь этого отеля на эти даты");
        }
        long overlap = bookingRepository.countOverlapping(hotel.getId(), req.checkIn(), req.checkOut());
        if (overlap >= hotel.getRoomsAvailable()) {
            throw new ConflictException("Нет свободных номеров на эти даты");
        }

        User user = userRepository.findById(userId).orElseThrow();
        long nights = ChronoUnit.DAYS.between(req.checkIn(), req.checkOut());
        BigDecimal total = hotel.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(req.rooms()));

        // открываем чат «турист ↔ отель»
        Chat chat = null;
        if (hotel.getOwner() != null && !hotel.getOwner().getId().equals(userId)) {
            chat = chatService.createPersonal(userId, hotel.getOwner().getId());
        }

        Booking b = Booking.builder()
                .hotel(hotel).user(user)
                .checkIn(req.checkIn()).checkOut(req.checkOut())
                .guests(req.guests()).rooms(req.rooms())
                .totalPrice(total).currency(hotel.getCurrency())
                .status(BookingStatus.PENDING)
                .chat(chat)
                .build();
        Booking saved = bookingRepository.save(b);

        // системное сообщение в чат + уведомление владельцу
        if (chat != null) {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("bookingId", saved.getId());
            payload.put("hotelId", hotel.getId());
            payload.put("hotelName", hotel.getName());
            payload.put("checkIn", saved.getCheckIn().toString());
            payload.put("checkOut", saved.getCheckOut().toString());
            payload.put("guests", (int) saved.getGuests());
            payload.put("rooms", (int) saved.getRooms());
            payload.put("totalPrice", saved.getTotalPrice());
            payload.put("status", saved.getStatus().name());
            messageService.send(chat.getId(), userId,
                    new messenger.bereza.web.dto.message.SendMessageRequest(
                            MessageType.BOOKING,
                            "Новая бронь №" + saved.getId(),
                            payload,
                            null, null));
        }
        if (hotel.getOwner() != null) {
            notificationService.push(hotel.getOwner().getId(),
                    "BOOKING_NEW",
                    "Новая бронь",
                    "Гость " + user.getDisplayName() + " забронировал " + hotel.getName(),
                    Map.of("bookingId", saved.getId()));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Booking> myBookings(Long userId, Pageable pageable) {
        return bookingRepository.findAllByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Booking> incomingBookings(Long hotelOwnerId, Pageable pageable) {
        return bookingRepository.findAllByHotelOwnerId(hotelOwnerId, pageable);
    }

    @Transactional(readOnly = true)
    public Booking get(Long id, Long requesterId) {
        Booking b = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Бронь не найдена"));
        ensureRelated(b, requesterId);
        return b;
    }

    @Transactional
    public Booking changeStatus(Long bookingId, Long requesterId, BookingStatus newStatus) {
        Booking b = get(bookingId, requesterId);
        boolean isGuest = b.getUser().getId().equals(requesterId);
        boolean isHotelOwner = b.getHotel().getOwner() != null
                && b.getHotel().getOwner().getId().equals(requesterId);
        // Турист может только отменять; владелец отеля может все переходы
        if (isGuest && !isHotelOwner && newStatus != BookingStatus.CANCELLED) {
            throw new ForbiddenException("Турист может только отменить бронь");
        }

        b.setStatus(newStatus);

        if (b.getChat() != null) {
            messageService.send(b.getChat().getId(),
                    isHotelOwner ? b.getHotel().getOwner().getId() : b.getUser().getId(),
                    new messenger.bereza.web.dto.message.SendMessageRequest(
                            MessageType.SYSTEM,
                            "Статус брони №" + b.getId() + ": " + newStatus.name(),
                            Map.of("bookingId", b.getId(), "status", newStatus.name()),
                            null, null));
        }
        // уведомить «другую сторону»
        Long notifyUser = isHotelOwner ? b.getUser().getId()
                : (b.getHotel().getOwner() != null ? b.getHotel().getOwner().getId() : null);
        if (notifyUser != null) {
            notificationService.push(notifyUser, "BOOKING_STATUS",
                    "Изменился статус брони",
                    "Бронь №" + b.getId() + " — " + newStatus.name(),
                    Map.of("bookingId", b.getId(), "status", newStatus.name()));
        }
        return b;
    }

    private void ensureRelated(Booking b, Long userId) {
        boolean isGuest = b.getUser().getId().equals(userId);
        boolean isHotelOwner = b.getHotel().getOwner() != null
                && b.getHotel().getOwner().getId().equals(userId);
        User u = userRepository.findById(userId).orElseThrow();
        if (!isGuest && !isHotelOwner && u.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Нет прав на просмотр брони");
        }
    }
}

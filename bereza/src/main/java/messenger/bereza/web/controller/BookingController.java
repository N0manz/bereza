package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.BookingService;
import messenger.bereza.web.dto.PageResponse;
import messenger.bereza.web.dto.booking.BookingResponse;
import messenger.bereza.web.dto.booking.CreateBookingRequest;
import messenger.bereza.web.dto.booking.UpdateBookingStatusRequest;
import messenger.bereza.web.mapper.BookingMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final CurrentUserProvider currentUser;

    @PostMapping
    @PreAuthorize("hasAnyRole('TOURIST','GUIDE','ADMIN')")
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest req) {
        return bookingMapper.toResponse(bookingService.create(currentUser.currentUserId(), req));
    }

    @GetMapping("/my")
    public PageResponse<BookingResponse> my(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = bookingService.myBookings(currentUser.currentUserId(),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(p.map(bookingMapper::toResponse));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasAnyRole('HOTEL','ADMIN')")
    public PageResponse<BookingResponse> incoming(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = bookingService.incomingBookings(currentUser.currentUserId(),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(p.map(bookingMapper::toResponse));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable Long id) {
        return bookingMapper.toResponse(bookingService.get(id, currentUser.currentUserId()));
    }

    @PostMapping("/{id}/status")
    public BookingResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateBookingStatusRequest req) {
        return bookingMapper.toResponse(
                bookingService.changeStatus(id, currentUser.currentUserId(), req.status()));
    }
}

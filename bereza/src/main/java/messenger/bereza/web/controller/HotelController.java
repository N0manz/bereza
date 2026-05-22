package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.HotelService;
import messenger.bereza.web.dto.PageResponse;
import messenger.bereza.web.dto.hotel.HotelResponse;
import messenger.bereza.web.dto.hotel.HotelUpsertRequest;
import messenger.bereza.web.mapper.HotelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final HotelMapper hotelMapper;
    private final CurrentUserProvider currentUser;

    @GetMapping
    public PageResponse<HotelResponse> search(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Short minStars,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = hotelService.search(city, minPrice, maxPrice, minStars,
                PageRequest.of(page, Math.min(size, 100), Sort.by("pricePerNight")));
        return PageResponse.of(p.map(hotelMapper::toResponse));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('HOTEL','ADMIN')")
    public java.util.List<HotelResponse> my() {
        return hotelService.myHotels(currentUser.currentUserId()).stream()
                .map(hotelMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public HotelResponse get(@PathVariable Long id) {
        return hotelMapper.toResponse(hotelService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOTEL','ADMIN')")
    public HotelResponse create(@Valid @RequestBody HotelUpsertRequest req) {
        return hotelMapper.toResponse(hotelService.create(currentUser.currentUserId(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOTEL','ADMIN')")
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelUpsertRequest req) {
        return hotelMapper.toResponse(hotelService.update(id, currentUser.currentUserId(), req));
    }
}

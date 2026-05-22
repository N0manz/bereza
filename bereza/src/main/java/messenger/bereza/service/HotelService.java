package messenger.bereza.service;

import lombok.RequiredArgsConstructor;
import messenger.bereza.domain.Hotel;
import messenger.bereza.domain.Role;
import messenger.bereza.domain.User;
import messenger.bereza.exception.ForbiddenException;
import messenger.bereza.exception.NotFoundException;
import messenger.bereza.repository.HotelRepository;
import messenger.bereza.repository.UserRepository;
import messenger.bereza.web.dto.hotel.HotelUpsertRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<Hotel> search(String city, BigDecimal minPrice, BigDecimal maxPrice,
                              Short minStars, Pageable pageable) {
        return hotelRepository.search(city, minPrice, maxPrice, minStars, pageable);
    }

    @Transactional(readOnly = true)
    public java.util.List<Hotel> myHotels(Long ownerId) {
        return hotelRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public Hotel get(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Отель не найден"));
    }

    @Transactional
    public Hotel create(Long requesterId, HotelUpsertRequest req) {
        User owner = userRepository.findById(requesterId).orElseThrow();
        if (owner.getRole() != Role.HOTEL && owner.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Только пользователи с ролью HOTEL могут создавать отели");
        }
        Hotel h = Hotel.builder()
                .owner(owner)
                .name(req.name())
                .description(req.description())
                .city(req.city())
                .address(req.address())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .stars(req.stars())
                .pricePerNight(req.pricePerNight())
                .currency(req.currency() != null ? req.currency() : "RUB")
                .roomsAvailable(req.roomsAvailable() != null ? req.roomsAvailable() : 0)
                .photos(req.photos() != null ? new ArrayList<>(req.photos()) : new ArrayList<>())
                .amenities(req.amenities() != null ? new ArrayList<>(req.amenities()) : new ArrayList<>())
                .active(req.active() == null || req.active())
                .build();
        return hotelRepository.save(h);
    }

    @Transactional
    public Hotel update(Long hotelId, Long requesterId, HotelUpsertRequest req) {
        Hotel h = get(hotelId);
        ensureOwnerOrAdmin(h, requesterId);
        h.setName(req.name());
        h.setDescription(req.description());
        h.setCity(req.city());
        h.setAddress(req.address());
        h.setLatitude(req.latitude());
        h.setLongitude(req.longitude());
        h.setStars(req.stars());
        h.setPricePerNight(req.pricePerNight());
        if (req.currency() != null) h.setCurrency(req.currency());
        if (req.roomsAvailable() != null) h.setRoomsAvailable(req.roomsAvailable());
        if (req.photos() != null) h.setPhotos(new ArrayList<>(req.photos()));
        if (req.amenities() != null) h.setAmenities(new ArrayList<>(req.amenities()));
        if (req.active() != null) h.setActive(req.active());
        return h;
    }

    private void ensureOwnerOrAdmin(Hotel h, Long requesterId) {
        User u = userRepository.findById(requesterId).orElseThrow();
        boolean isOwner = h.getOwner() != null && h.getOwner().getId().equals(requesterId);
        if (!isOwner && u.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Нет прав на изменение отеля");
        }
    }
}

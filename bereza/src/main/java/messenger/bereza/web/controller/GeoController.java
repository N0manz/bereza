package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.GeoService;
import messenger.bereza.web.dto.geo.CreateGeoPointRequest;
import messenger.bereza.web.dto.geo.GeoPointResponse;
import messenger.bereza.web.mapper.GeoPointMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;
    private final GeoPointMapper mapper;
    private final CurrentUserProvider currentUser;

    @PostMapping("/points")
    @PreAuthorize("hasAnyRole('GUIDE','HOTEL','ADMIN')")
    public GeoPointResponse create(@Valid @RequestBody CreateGeoPointRequest req) {
        return mapper.toResponse(geoService.create(currentUser.currentUserId(), req));
    }

    @GetMapping("/chats/{chatId}")
    public List<GeoPointResponse> listForChat(@PathVariable Long chatId) {
        return geoService.activeInChat(chatId, currentUser.currentUserId())
                .stream().map(mapper::toResponse).toList();
    }
}

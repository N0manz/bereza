package messenger.bereza.api;

import com.fasterxml.jackson.databind.JsonNode;
import messenger.bereza.AbstractIT;
import messenger.bereza.domain.BookingStatus;
import messenger.bereza.web.dto.booking.CreateBookingRequest;
import messenger.bereza.web.dto.booking.UpdateBookingStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingApiIT extends AbstractIT {

    // Смещение дат, чтобы брони в разных тестах не пересекались
    private static final AtomicInteger DAY_OFFSET = new AtomicInteger(10);

    private long getSuzdhalHotelId() throws Exception {
        String body = mockMvc.perform(get("/api/hotels").param("city", "Суздаль"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content").get(0).get("id").asLong();
    }

    private CreateBookingRequest nextBookingRequest(long hotelId) {
        int base = DAY_OFFSET.getAndAdd(10);
        return new CreateBookingRequest(
                hotelId,
                LocalDate.now().plusDays(base),
                LocalDate.now().plusDays(base + 3),
                (short) 1, (short) 1);
    }

    @Test
    void tourist_createsBooking_withPendingStatus() throws Exception {
        long hotelId = getSuzdhalHotelId();
        var session = loginAs("tourist", "demo1234");

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextBookingRequest(hotelId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.hotelId").value(hotelId))
                .andExpect(jsonPath("$.totalPrice").isNumber());
    }

    @Test
    void myBookings_returnsList_afterCreation() throws Exception {
        long hotelId = getSuzdhalHotelId();
        var session = loginAs("tourist", "demo1234");

        // создаём бронь
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextBookingRequest(hotelId))))
                .andExpect(status().isOk());

        // список должен содержать хотя бы эту бронь
        mockMvc.perform(get("/api/bookings/my").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }

    @Test
    void hotelOwner_canConfirm_booking() throws Exception {
        long hotelId = getSuzdhalHotelId();
        var touristSession = loginAs("tourist", "demo1234");
        var hotelSession  = loginAs("hotel", "demo1234");

        // турист создаёт бронь
        String json = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(touristSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextBookingRequest(hotelId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long bookingId = objectMapper.readTree(json).get("id").asLong();

        // отель подтверждает
        mockMvc.perform(post("/api/bookings/{id}/status", bookingId)
                        .with(csrf())
                        .cookie(hotelSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateBookingStatusRequest(BookingStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void tourist_canCancel_ownBooking() throws Exception {
        long hotelId = getSuzdhalHotelId();
        var session = loginAs("tourist", "demo1234");

        String json = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nextBookingRequest(hotelId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long bookingId = objectMapper.readTree(json).get("id").asLong();

        mockMvc.perform(post("/api/bookings/{id}/status", bookingId)
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateBookingStatusRequest(BookingStatus.CANCELLED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createBooking_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateBookingRequest(1L,
                                        LocalDate.now().plusDays(1),
                                        LocalDate.now().plusDays(2),
                                        (short) 1, (short) 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incomingBookings_returns403_forTourist() throws Exception {
        var session = loginAs("tourist", "demo1234");

        mockMvc.perform(get("/api/bookings/incoming").cookie(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void incomingBookings_returns200_forHotelRole() throws Exception {
        var session = loginAs("hotel", "demo1234");

        mockMvc.perform(get("/api/bookings/incoming").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}

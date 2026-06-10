package messenger.bereza.api;

import messenger.bereza.AbstractIT;
import messenger.bereza.web.dto.hotel.HotelUpsertRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HotelApiIT extends AbstractIT {

    @Test
    void listHotels_isPublic_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
    }

    @Test
    void listHotels_filtersByCity() throws Exception {
        mockMvc.perform(get("/api/hotels").param("city", "Суздаль"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Суздаль"));
    }

    @Test
    void getHotel_returns200_withoutAuth() throws Exception {
        // получаем id первого отеля и запрашиваем напрямую
        String body = mockMvc.perform(get("/api/hotels"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("content").get(0).get("id").asLong();

        mockMvc.perform(get("/api/hotels/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void createHotel_returns200_forHotelRole() throws Exception {
        var session = loginAs("hotel", "demo1234");

        HotelUpsertRequest req = new HotelUpsertRequest(
                "Тест Гостиница", "Описание", "Новосибирск", "пр. Ленина, 1",
                54.989, 82.894, (short) 3,
                BigDecimal.valueOf(4500), "RUB", 10,
                null, null, true);

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Тест Гостиница"))
                .andExpect(jsonPath("$.city").value("Новосибирск"));
    }

    @Test
    void createHotel_returns403_forTouristRole() throws Exception {
        var session = loginAs("tourist", "demo1234");

        HotelUpsertRequest req = new HotelUpsertRequest(
                "Запрещено", null, "Казань", "ул. Баумана, 1",
                null, null, null,
                BigDecimal.valueOf(3000), "RUB", 1,
                null, null, true);

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createHotel_returns401_withoutAuth() throws Exception {
        HotelUpsertRequest req = new HotelUpsertRequest(
                "Без авторизации", null, "Казань", "ул. Кремлёвская, 1",
                null, null, null,
                BigDecimal.valueOf(3000), "RUB", 1,
                null, null, true);

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}

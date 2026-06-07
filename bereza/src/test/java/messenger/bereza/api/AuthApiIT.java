package messenger.bereza.api;

import messenger.bereza.AbstractIT;
import messenger.bereza.domain.Role;
import messenger.bereza.web.dto.auth.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthApiIT extends AbstractIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String next() {
        return "it_usr_" + SEQ.incrementAndGet();
    }

    @Test
    void register_returns201_withValidPayload() throws Exception {
        String u = next();
        RegisterRequest req = new RegisterRequest(u, u + "@test.com", "password123", "Тест", Role.TOURIST, null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(u))
                .andExpect(jsonPath("$.role").value("TOURIST"));
    }

    @Test
    void register_returns400_whenUsernameTooShort() throws Exception {
        RegisterRequest req = new RegisterRequest("ab", "ab@test.com", "password123", "Тест", Role.TOURIST, null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        String u = next();
        RegisterRequest req = new RegisterRequest(u, u + "@test.com", "short", "Тест", Role.TOURIST, null);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200_withValidCredentials() throws Exception {
        // seed-данные: tourist / demo1234
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "tourist")
                        .param("password", "demo1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tourist"))
                .andExpect(jsonPath("$.role").value("TOURIST"));
    }

    @Test
    void login_returns401_withWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "tourist")
                        .param("password", "неверный_пароль"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsCurrentUser_whenAuthenticated() throws Exception {
        var session = loginAs("guide", "demo1234");

        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("guide"))
                .andExpect(jsonPath("$.role").value("GUIDE"));
    }

    @Test
    void me_returnsNull_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }
}

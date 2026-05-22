package messenger.bereza.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.BerezaUserDetails;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.AuthService;
import messenger.bereza.service.UserService;
import messenger.bereza.web.dto.auth.AuthResponse;
import messenger.bereza.web.dto.auth.RegisterRequest;
import messenger.bereza.web.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CurrentUserProvider currentUser;
    private final UserMapper userMapper;

    /** Получить CSRF-токен (cookie XSRF-TOKEN выставляется автоматически). */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ResponseEntity.ok(Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()
        ));
    }

    /** Регистрация нового пользователя. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        var user = authService.register(req);
        return ResponseEntity.status(201).body(userMapper.toAuth(user));
    }

    /**
     * Логин обрабатывается Spring Security UsernamePasswordAuthenticationFilter
     * по адресу POST /api/auth/login (см. SecurityConfig). На успех — JSON c пользователем.
     * Этот метод существует только для документации Swagger в будущем.
     */
    @PostMapping("/login")
    public void loginPlaceholder() {
        // обрабатывается фильтром Spring Security
    }

    /** Получить текущего пользователя; для гостя — 200 + null. */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof BerezaUserDetails p)) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(userMapper.toAuth(userService.get(p.getId())));
    }
}

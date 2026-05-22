package messenger.bereza.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import messenger.bereza.security.CurrentUserProvider;
import messenger.bereza.service.UserService;
import messenger.bereza.web.dto.PageResponse;
import messenger.bereza.web.dto.user.UpdateUserRequest;
import messenger.bereza.web.dto.user.UserResponse;
import messenger.bereza.web.mapper.UserMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUser;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public UserResponse me() {
        return userMapper.toResponse(userService.get(currentUser.currentUserId()));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateUserRequest req) {
        return userMapper.toResponse(userService.update(currentUser.currentUserId(), req));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userMapper.toResponse(userService.get(id));
    }

    @GetMapping
    public PageResponse<UserResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var p = userService.search(q, PageRequest.of(page, Math.min(size, 100), Sort.by("displayName")));
        return PageResponse.of(p.map(userMapper::toResponse));
    }
}

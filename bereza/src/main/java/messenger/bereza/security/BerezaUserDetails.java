package messenger.bereza.security;

import lombok.Getter;
import messenger.bereza.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Lightweight principal stored in the HTTP session. Не храним полноценную JPA-сущность —
 * только то, что нужно для авторизации/идентификации (snapshot на момент логина).
 */
@Getter
public class BerezaUserDetails implements UserDetails, Serializable {

    private final Long id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final String role;
    private final boolean enabled;
    private final boolean locked;

    public BerezaUserDetails(User u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.displayName = u.getDisplayName();
        this.passwordHash = u.getPasswordHash();
        this.role = u.getRole().name();
        this.enabled = u.isEnabled();
        this.locked = u.isLocked();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override public String getPassword()                  { return passwordHash; }
    @Override public boolean isAccountNonExpired()         { return true; }
    @Override public boolean isAccountNonLocked()          { return !locked; }
    @Override public boolean isCredentialsNonExpired()     { return true; }
    @Override public boolean isEnabled()                   { return enabled; }
}

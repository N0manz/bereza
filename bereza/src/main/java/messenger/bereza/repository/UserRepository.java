package messenger.bereza.repository;

import messenger.bereza.domain.Role;
import messenger.bereza.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE lower(u.username) = lower(:username)")
    Optional<User> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("""
           SELECT u FROM User u
           WHERE lower(u.username) LIKE lower(concat('%', :q, '%'))
              OR lower(u.displayName) LIKE lower(concat('%', :q, '%'))
              OR lower(u.email) LIKE lower(concat('%', :q, '%'))
           """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}

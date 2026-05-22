package messenger.bereza.repository;

import messenger.bereza.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    @Query("""
           SELECT h FROM Hotel h
           WHERE h.active = true
             AND (:city IS NULL OR lower(h.city) = lower(cast(:city as string)))
             AND (:minPrice IS NULL OR h.pricePerNight >= :minPrice)
             AND (:maxPrice IS NULL OR h.pricePerNight <= :maxPrice)
             AND (:minStars IS NULL OR h.stars >= :minStars)
           """)
    Page<Hotel> search(@Param("city") String city,
                       @Param("minPrice") BigDecimal minPrice,
                       @Param("maxPrice") BigDecimal maxPrice,
                       @Param("minStars") Short minStars,
                       Pageable pageable);
}

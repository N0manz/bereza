package messenger.bereza.repository;

import messenger.bereza.domain.Booking;
import messenger.bereza.domain.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query(value = """
           SELECT b FROM Booking b
           JOIN FETCH b.hotel h
           JOIN FETCH b.user u
           WHERE u.id = :userId
           """,
           countQuery = "SELECT count(b) FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
           SELECT b FROM Booking b
           JOIN FETCH b.hotel h
           JOIN FETCH b.user u
           WHERE h.owner.id = :ownerId
           """,
           countQuery = "SELECT count(b) FROM Booking b WHERE b.hotel.owner.id = :ownerId")
    Page<Booking> findAllByHotelOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    Page<Booking> findAllByStatus(BookingStatus status, Pageable pageable);

    @Query("""
           SELECT count(b) FROM Booking b
           WHERE b.hotel.id = :hotelId
             AND b.status IN ('PENDING', 'CONFIRMED')
             AND b.checkIn < :checkOut AND b.checkOut > :checkIn
           """)
    long countOverlapping(@Param("hotelId") Long hotelId,
                          @Param("checkIn") java.time.LocalDate checkIn,
                          @Param("checkOut") java.time.LocalDate checkOut);

    @Query("""
           SELECT count(b) FROM Booking b
           WHERE b.hotel.id = :hotelId
             AND b.user.id = :userId
             AND b.status IN ('PENDING', 'CONFIRMED')
             AND b.checkIn < :checkOut AND b.checkOut > :checkIn
           """)
    long countOverlappingForUser(@Param("hotelId") Long hotelId,
                                 @Param("userId") Long userId,
                                 @Param("checkIn") java.time.LocalDate checkIn,
                                 @Param("checkOut") java.time.LocalDate checkOut);
}

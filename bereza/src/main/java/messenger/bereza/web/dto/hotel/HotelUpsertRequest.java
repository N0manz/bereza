package messenger.bereza.web.dto.hotel;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record HotelUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 500) String address,
        @DecimalMin("-90") @DecimalMax("90")   Double latitude,
        @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @Min(1) @Max(5)                         Short stars,
        @NotNull @DecimalMin("0")               BigDecimal pricePerNight,
        @Size(min = 3, max = 3)                 String currency,
        @Min(0)                                 Integer roomsAvailable,
        List<String> photos,
        List<String> amenities,
        Boolean active
) {}

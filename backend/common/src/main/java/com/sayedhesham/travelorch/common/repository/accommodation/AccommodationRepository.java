package com.sayedhesham.travelorch.common.repository.accommodation;

import com.sayedhesham.travelorch.common.entity.accommodation.Accommodation;
import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.enums.AccommodationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    
    List<Accommodation> findByDestination(Destination destination);
    
    List<Accommodation> findByDestinationAndType(Destination destination, AccommodationType type);
    
    List<Accommodation> findByPricePerNightLessThanEqual(BigDecimal maxPrice);
}

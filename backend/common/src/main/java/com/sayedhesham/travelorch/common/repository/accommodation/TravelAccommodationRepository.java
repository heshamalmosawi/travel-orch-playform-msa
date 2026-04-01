package com.sayedhesham.travelorch.common.repository.accommodation;

import com.sayedhesham.travelorch.common.entity.accommodation.TravelAccommodation;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelAccommodationRepository extends JpaRepository<TravelAccommodation, Long> {
    
    List<TravelAccommodation> findByTravel(Travel travel);
    
    void deleteByTravel(Travel travel);
}

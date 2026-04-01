package com.sayedhesham.travelorch.common.repository.travel;

import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.travel.TravelDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelDestinationRepository extends JpaRepository<TravelDestination, Long> {
    
    List<TravelDestination> findByTravelOrderByVisitOrderAsc(Travel travel);
    
    void deleteByTravel(Travel travel);
}

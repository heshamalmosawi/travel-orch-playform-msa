package com.sayedhesham.travelorch.common.repository.transportation;

import com.sayedhesham.travelorch.common.entity.transportation.TransportationSegment;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportationSegmentRepository extends JpaRepository<TransportationSegment, Long> {
    
    List<TransportationSegment> findByTravel(Travel travel);
    
    void deleteByTravel(Travel travel);
}

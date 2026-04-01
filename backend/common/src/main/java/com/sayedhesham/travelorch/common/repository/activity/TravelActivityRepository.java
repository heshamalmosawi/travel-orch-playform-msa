package com.sayedhesham.travelorch.common.repository.activity;

import com.sayedhesham.travelorch.common.entity.activity.TravelActivity;
import com.sayedhesham.travelorch.common.entity.travel.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelActivityRepository extends JpaRepository<TravelActivity, Long> {
    
    List<TravelActivity> findByTravel(Travel travel);
    
    List<TravelActivity> findByTravelAndScheduledDate(Travel travel, LocalDate date);
    
    void deleteByTravel(Travel travel);
}

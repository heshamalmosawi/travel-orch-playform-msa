package com.sayedhesham.travelorch.common.repository.activity;

import com.sayedhesham.travelorch.common.entity.activity.Activity;
import com.sayedhesham.travelorch.common.entity.travel.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    List<Activity> findByDestination(Destination destination);
    
    List<Activity> findByCategory(String category);
    
    List<Activity> findByDestinationAndCategory(Destination destination, String category);
}

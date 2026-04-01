package com.sayedhesham.travelorch.common.repository.travel;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import com.sayedhesham.travelorch.common.entity.travel.DestinationConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationConnectionRepository extends JpaRepository<DestinationConnection, Long> {
    
    List<DestinationConnection> findByFromDestination(Destination from);
    
    List<DestinationConnection> findByToDestination(Destination to);
    
    Optional<DestinationConnection> findByFromDestinationAndToDestination(Destination from, Destination to);
}

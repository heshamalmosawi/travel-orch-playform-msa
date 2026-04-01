package com.sayedhesham.travelorch.common.repository.travel;

import com.sayedhesham.travelorch.common.entity.travel.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    
    List<Destination> findByCountry(String country);
    
    List<Destination> findByCity(String city);
    
    List<Destination> findByCountryAndCity(String country, String city);
    
    List<Destination> findByNameContainingIgnoreCase(String name);
}

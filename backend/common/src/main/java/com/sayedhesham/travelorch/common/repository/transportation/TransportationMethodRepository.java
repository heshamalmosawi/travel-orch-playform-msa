package com.sayedhesham.travelorch.common.repository.transportation;

import com.sayedhesham.travelorch.common.entity.transportation.TransportationMethod;
import com.sayedhesham.travelorch.common.enums.TransportationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransportationMethodRepository extends JpaRepository<TransportationMethod, Long> {
    
    List<TransportationMethod> findByType(TransportationType type);
}

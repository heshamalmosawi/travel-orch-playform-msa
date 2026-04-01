package com.sayedhesham.travelorch.common.repository.travel;

import com.sayedhesham.travelorch.common.entity.travel.Travel;
import com.sayedhesham.travelorch.common.entity.user.User;
import com.sayedhesham.travelorch.common.enums.TravelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TravelRepository extends JpaRepository<Travel, Long> {
    
    List<Travel> findByUser(User user);
    
    List<Travel> findByUserAndStatus(User user, TravelStatus status);
    
    List<Travel> findByStatus(TravelStatus status);
    
    List<Travel> findByStartDateBetween(LocalDate start, LocalDate end);
    
    @Query("SELECT t FROM Travel t JOIN FETCH t.destinations WHERE t.id = :id")
    Travel findByIdWithDestinations(@Param("id") Long id);
    
    @Query("SELECT t FROM Travel t WHERE t.user = :user AND t.startDate >= :date ORDER BY t.startDate ASC")
    List<Travel> findUpcomingTravels(@Param("user") User user, @Param("date") LocalDate date);
}

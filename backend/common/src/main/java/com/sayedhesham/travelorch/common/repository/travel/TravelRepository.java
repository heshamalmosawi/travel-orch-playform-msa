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
    
    List<Travel> findByManager(User manager);

    List<Travel> findByManagerAndStatus(User manager, TravelStatus status);

    List<Travel> findByStatus(TravelStatus status);

    List<Travel> findByStartDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT t FROM Travel t LEFT JOIN FETCH t.destinations WHERE t.id = :id")
    Travel findByIdWithDestinations(@Param("id") Long id);

    @Query("SELECT DISTINCT t FROM Travel t LEFT JOIN FETCH t.destinations td LEFT JOIN FETCH td.destination WHERE t.manager = :manager AND t.startDate >= :date ORDER BY t.startDate ASC")
    List<Travel> findUpcomingTravels(@Param("manager") User manager, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT t FROM Travel t LEFT JOIN FETCH t.destinations td LEFT JOIN FETCH td.destination WHERE t.startDate >= :date AND t.status <> :excludedStatus ORDER BY t.startDate ASC")
    List<Travel> findAllUpcomingTravels(@Param("date") LocalDate date, @Param("excludedStatus") TravelStatus excludedStatus);
}

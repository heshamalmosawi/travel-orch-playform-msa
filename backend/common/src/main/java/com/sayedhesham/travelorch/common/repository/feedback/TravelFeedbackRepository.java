package com.sayedhesham.travelorch.common.repository.feedback;

import com.sayedhesham.travelorch.common.entity.feedback.TravelFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelFeedbackRepository extends JpaRepository<TravelFeedback, Long> {

    List<TravelFeedback> findByTravelId(Long travelId);

    List<TravelFeedback> findByManagerId(Long managerId);

    Optional<TravelFeedback> findByTravelIdAndReviewerId(Long travelId, Long reviewerId);

    boolean existsByTravelIdAndReviewerId(Long travelId, Long reviewerId);

    List<TravelFeedback> findByReviewerId(Long reviewerId);
}

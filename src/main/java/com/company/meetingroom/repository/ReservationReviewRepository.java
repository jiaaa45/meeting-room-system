package com.company.meetingroom.repository;

import com.company.meetingroom.entity.ReservationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationReviewRepository extends JpaRepository<ReservationReview, Long> {
}

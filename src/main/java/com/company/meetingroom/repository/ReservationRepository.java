package com.company.meetingroom.repository;

import com.company.meetingroom.entity.Reservation;
import com.company.meetingroom.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation>  {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.room.id = :roomId
          AND r.status IN :blockingStatuses
          AND r.startTime < :endTime
          AND r.endTime > :startTime
        """)
    List<Reservation> findConflictingReservations(
        @Param("roomId") Long roomId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("blockingStatuses") List<ReservationStatus> blockingStatuses
    );
    List<Reservation> findByRoomIdOrderByStartTimeAsc(Long roomId);
    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.user
        WHERE r.status = :status
        AND r.startTime >= :dayStart
        AND r.startTime < :dayEnd
        """)
    List<Reservation> findApprovedReservationsForTimeline(
            @Param("status") ReservationStatus status,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );
}

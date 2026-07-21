package com.company.meetingroom.repository;

import com.company.meetingroom.entity.Reservation;
import com.company.meetingroom.entity.ReservationStatus;
import com.company.meetingroom.repository.projection.RoomUsageProjection;
import com.company.meetingroom.repository.projection.StatusCountProjection;

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

    @Query("""
        SELECT r.status AS status, COUNT(r) AS count
        FROM Reservation r
        WHERE r.startTime >= :monthStart AND r.startTime < :monthEnd
        GROUP BY r.status
        """)
    List<StatusCountProjection> countByStatusInMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd
    );

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.room
        JOIN FETCH r.user
        WHERE r.startTime >= :monthStart AND r.startTime < :monthEnd
        ORDER BY r.startTime
        """)
    List<Reservation> findAllInMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd
    );

    @Query(value = """
        SELECT r.room_id AS roomId, rm.name AS roomName,
            COUNT(*) AS reservationCount,
            CAST(SUM(EXTRACT(EPOCH FROM (r.end_time - r.start_time)) / 60) AS BIGINT) AS totalReservedMinutes
        FROM reservations r
        JOIN rooms rm ON rm.id = r.room_id
        WHERE r.status = 'APPROVED'
        AND r.start_time >= :monthStart AND r.start_time < :monthEnd
        GROUP BY r.room_id, rm.name
        ORDER BY reservationCount DESC
        LIMIT 3
        """, nativeQuery = true)
    List<RoomUsageProjection> findTopUsedRooms(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd
    );
}

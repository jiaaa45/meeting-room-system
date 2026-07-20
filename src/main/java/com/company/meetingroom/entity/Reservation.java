package com.company.meetingroom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

import com.company.meetingroom.exception.ForbiddenException;
import com.company.meetingroom.exception.InvalidReservationException;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "attendee_count", nullable = false)
    private Integer attendeeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private ReservationStatus previousStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void requestCancel(Long requestingUserId) {
        if (!this.user.getId().equals(requestingUserId)) {
            throw new ForbiddenException("只有預約本人可以申請退回");
        }
        if (this.status == ReservationStatus.REJECTED || this.status == ReservationStatus.CANCELLED) {
            throw new InvalidReservationException("此預約狀態不可再次申請退回");
        }
        this.previousStatus = this.status;
        this.status = ReservationStatus.CANCEL_REQUESTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void approveCancelRequest() {
        this.status = ReservationStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void rejectCancelRequest() {
        this.status = this.previousStatus;
        this.previousStatus = null;
        this.updatedAt = LocalDateTime.now();
    }
}
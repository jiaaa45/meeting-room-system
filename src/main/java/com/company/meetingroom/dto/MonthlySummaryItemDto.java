package com.company.meetingroom.dto;

import com.company.meetingroom.entity.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MonthlySummaryItemDto {
    private Long reservationId;
    private String roomName;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
}
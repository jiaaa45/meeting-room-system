package com.company.meetingroom.dto;

import com.company.meetingroom.entity.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReservationResponseDto {

    private Long id;
    private String roomName;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String subject;
    private String purpose;
    private Integer attendeeCount;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
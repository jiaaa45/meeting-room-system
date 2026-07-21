package com.company.meetingroom.dto;

import com.company.meetingroom.entity.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimelineReservationDto {
    private Long reservationId;
    private String startTime;
    private String endTime;
    private String username;
    private String subject;
    private ReservationStatus status;
}

package com.company.meetingroom.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TimelineRoomDto {
    private Long roomId;
    private String roomName;
    private Integer capacity;
    private List<TimelineReservationDto> reservations;
}

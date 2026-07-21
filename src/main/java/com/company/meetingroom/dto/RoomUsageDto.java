package com.company.meetingroom.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomUsageDto {
    private Long roomId;
    private String roomName;
    private Long reservationCount;
    private Long totalReservedMinutes;
}
package com.company.meetingroom.repository.projection;

public interface RoomUsageProjection {
    Long getRoomId();
    String getRoomName();
    Long getReservationCount();
    Long getTotalReservedMinutes();
}
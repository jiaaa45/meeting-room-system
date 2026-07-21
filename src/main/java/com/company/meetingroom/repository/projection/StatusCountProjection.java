package com.company.meetingroom.repository.projection;

import com.company.meetingroom.entity.ReservationStatus;

public interface StatusCountProjection {
    ReservationStatus getStatus();
    Long getCount();
}
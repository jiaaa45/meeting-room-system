package com.company.meetingroom.mapper;

import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.entity.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponseDto toResponseDto(Reservation reservation) {
        return ReservationResponseDto.builder()
                .id(reservation.getId())
                .roomName(reservation.getRoom().getName())
                .username(reservation.getUser().getUsername())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .subject(reservation.getSubject())
                .purpose(reservation.getPurpose())
                .attendeeCount(reservation.getAttendeeCount())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
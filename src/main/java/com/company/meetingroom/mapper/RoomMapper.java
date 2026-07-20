package com.company.meetingroom.mapper;

import com.company.meetingroom.dto.RoomRequestDto;
import com.company.meetingroom.dto.RoomResponseDto;
import com.company.meetingroom.entity.Room;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RoomMapper {

    public Room toEntity(RoomRequestDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return Room.builder()
                .name(dto.getName())
                .capacity(dto.getCapacity())
                .floor(dto.getFloor())
                .location(dto.getLocation())
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public RoomResponseDto toResponseDto(Room room) {
        return RoomResponseDto.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .floor(room.getFloor())
                .location(room.getLocation())
                .isActive(room.getIsActive())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
package com.company.meetingroom.service;

import com.company.meetingroom.dto.RoomRequestDto;
import com.company.meetingroom.dto.RoomResponseDto;
import com.company.meetingroom.entity.Room;
import com.company.meetingroom.exception.ResourceNotFoundException;
import com.company.meetingroom.mapper.RoomMapper;
import com.company.meetingroom.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Transactional
    public RoomResponseDto create(RoomRequestDto requestDto) {
        Room room = roomMapper.toEntity(requestDto);
        Room saved = roomRepository.save(room);
        return roomMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public RoomResponseDto getById(Long id) {
        Room room = findRoomOrThrow(id);
        return roomMapper.toResponseDto(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDto> getAll() {
        return roomRepository.findAll()
                .stream()
                .map(roomMapper::toResponseDto)
                .toList();
    }

    @Transactional
    public RoomResponseDto update(Long id, RoomRequestDto requestDto) {
        Room room = findRoomOrThrow(id);
        room.setName(requestDto.getName());
        room.setCapacity(requestDto.getCapacity());
        room.setFloor(requestDto.getFloor());
        room.setLocation(requestDto.getLocation());
        room.setUpdatedAt(LocalDateTime.now());
        return roomMapper.toResponseDto(room);
    }

    @Transactional
    public void deactivate(Long id) {
        Room room = findRoomOrThrow(id);
        room.setIsActive(false);
        room.setUpdatedAt(LocalDateTime.now());
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + id + " 的會議室"));
    }
}
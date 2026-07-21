package com.company.meetingroom.controller;

import com.company.meetingroom.dto.RoomRequestDto;
import com.company.meetingroom.dto.RoomResponseDto;
import com.company.meetingroom.dto.RoomUsageDto;
import com.company.meetingroom.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<RoomResponseDto> create(@Valid @RequestBody RoomRequestDto requestDto) {
        RoomResponseDto created = roomService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponseDto>> getAll() {
        return ResponseEntity.ok(roomService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequestDto requestDto) {
        return ResponseEntity.ok(roomService.update(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        roomService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/reservations")
    public ResponseEntity<List<ReservationResponseDto>> getReservationsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(reservationService.getByRoomId(roomId));
    }

    @GetMapping("/top-used")
    public ResponseEntity<List<RoomUsageDto>> getTopUsedRooms(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(reservationService.getTopUsedRooms(year, month));
    }
}
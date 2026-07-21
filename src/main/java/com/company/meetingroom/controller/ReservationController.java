package com.company.meetingroom.controller;

import com.company.meetingroom.dto.CancelRequestDto;
import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.dto.ReviewRequestDto;
import com.company.meetingroom.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.company.meetingroom.entity.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.time.LocalDate;
import java.util.List;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.service.ReservationService;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    
    @PostMapping
    public ResponseEntity<ReservationResponseDto> create(
            @Valid @RequestBody ReservationRequestDto requestDto) {
        ReservationResponseDto created = reservationService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/cancel-request")
    public ResponseEntity<ReservationResponseDto> cancelRequest(
            @PathVariable Long id,
            @Valid @RequestBody CancelRequestDto requestDto) {
        return ResponseEntity.ok(reservationService.cancelRequest(id, requestDto));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ReservationResponseDto> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequestDto requestDto) {
        return ResponseEntity.ok(reservationService.review(id, requestDto));
    }

    @GetMapping
    public ResponseEntity<Page<ReservationResponseDto>> search(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String roomName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20, sort = "startTime") Pageable pageable) {
        return ResponseEntity.ok(
                reservationService.search(dateFrom, dateTo, roomId, roomName, username, status, pageable));
    }
}

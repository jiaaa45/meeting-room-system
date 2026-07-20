package com.company.meetingroom.controller;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

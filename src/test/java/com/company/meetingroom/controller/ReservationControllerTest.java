package com.company.meetingroom.controller;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.dto.TimelineResponseDto;
import com.company.meetingroom.dto.TimelineRoomDto;
import com.company.meetingroom.entity.ReservationStatus;
import com.company.meetingroom.exception.ReservationConflictException;
import com.company.meetingroom.exception.ResourceNotFoundException;
import com.company.meetingroom.service.ReservationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Autowired
    private ObjectMapper objectMapper;

    private ReservationRequestDto validRequest() {
        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setRoomId(1L);
        dto.setUserId(1L);
        dto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        dto.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0));
        dto.setSubject("測試會議");
        dto.setAttendeeCount(4);
        return dto;
    }

    @Test
    void createReservation_shouldReturn201_whenRequestIsValid() throws Exception {
        ReservationResponseDto responseDto = ReservationResponseDto.builder()
                .id(1L)
                .roomName("會議室 101")
                .username("王小明")
                .status(ReservationStatus.PROCESSING)
                .build();

        when(reservationService.create(any(ReservationRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomName").value("會議室 101"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void createReservation_shouldReturn400_whenSubjectIsBlank() throws Exception {
        ReservationRequestDto invalidRequest = validRequest();
        invalidRequest.setSubject("");  // 違反 @NotBlank

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_shouldReturn409_whenConflictExceptionThrown() throws Exception {
        when(reservationService.create(any(ReservationRequestDto.class)))
                .thenThrow(new ReservationConflictException("此會議室在該時段已被預約"));

        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("此會議室在該時段已被預約"));
    }

    @Test
    void getTimeline_shouldReturnCorrectJsonStructure() throws Exception {
        TimelineResponseDto timelineResponse = TimelineResponseDto.builder()
                .date(java.time.LocalDate.of(2026, 8, 10))
                .rooms(java.util.List.of(
                        TimelineRoomDto.builder()
                                .roomId(1L)
                                .roomName("會議室 101")
                                .capacity(6)
                                .reservations(java.util.List.of())
                                .build()
                ))
                .build();

        when(reservationService.getTimeline(any())).thenReturn(timelineResponse);

        mockMvc.perform(get("/api/reservations/timeline").param("date", "2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-10"))
                .andExpect(jsonPath("$.rooms[0].roomName").value("會議室 101"))
                .andExpect(jsonPath("$.rooms[0].reservations").isArray());
    }
}

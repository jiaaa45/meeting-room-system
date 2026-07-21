package com.company.meetingroom.controller;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
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
}

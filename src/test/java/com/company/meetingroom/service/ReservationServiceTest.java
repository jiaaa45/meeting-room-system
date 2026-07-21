package com.company.meetingroom.service;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.entity.*;
import com.company.meetingroom.exception.InvalidReservationException;
import com.company.meetingroom.exception.ReservationConflictException;
import com.company.meetingroom.exception.ResourceNotFoundException;
import com.company.meetingroom.mapper.ReservationMapper;
import com.company.meetingroom.repository.ReservationRepository;
import com.company.meetingroom.repository.ReservationReviewRepository;
import com.company.meetingroom.repository.RoomRepository;
import com.company.meetingroom.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationReviewRepository reservationReviewRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    private Room testRoom;
    private User testUser;
    private ReservationRequestDto validRequest;

    @BeforeEach
    void setUp() {
        testRoom = Room.builder()
                .id(1L)
                .name("會議室 101")
                .capacity(10)
                .isActive(true)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("王小明")
                .email("test@company.com")
                .role(Role.USER)
                .build();

        validRequest = new ReservationRequestDto();
        validRequest.setRoomId(1L);
        validRequest.setUserId(1L);
        validRequest.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        validRequest.setEndTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0));
        validRequest.setSubject("測試會議");
        validRequest.setAttendeeCount(5);
    }

    @Test
    void reserveRoom_shouldCreateReservation_whenRequestIsValid() {
        // Arrange(準備階段):告訴每個 Mock,當被呼叫時應該回傳什麼
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of());  // 沒有任何衝突
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));  // 直接回傳被存的那個物件
        when(reservationMapper.toResponseDto(any(Reservation.class)))
                .thenReturn(ReservationResponseDto.builder()
                        .id(100L)
                        .roomName("會議室 101")
                        .username("王小明")
                        .status(ReservationStatus.PROCESSING)
                        .build());

        // Act(執行階段):真正呼叫要測試的方法
        ReservationResponseDto result = reservationService.create(validRequest);

        // Assert(驗證階段):檢查結果對不對
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PROCESSING);
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }
    @Test
    void reserveRoom_shouldThrowNotFoundException_whenRoomDoesNotExist() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("找不到");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowNotFoundException_whenUserDoesNotExist() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowException_whenStartTimeAfterEndTime() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        validRequest.setStartTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0));
        validRequest.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("startTime 必須早於 endTime");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowException_whenReservingPastTime() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        validRequest.setStartTime(LocalDateTime.now().minusDays(1));
        validRequest.setEndTime(LocalDateTime.now().minusDays(1).plusHours(1));

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("不可預約過去時間");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowException_whenAttendeeCountExceedsCapacity() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        validRequest.setAttendeeCount(999);  // testRoom 容量是 10

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("超過會議室容量");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowConflictException_whenRoomAlreadyBooked() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of(mock(Reservation.class)));  // 假裝有一筆衝突的預約存在

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("已被預約");

        verify(reservationRepository, never()).save(any());
    }
}

package com.company.meetingroom.service;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.dto.ReviewRequestDto;
import com.company.meetingroom.entity.*;
import com.company.meetingroom.exception.ForbiddenException;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Test
    void reserveRoom_shouldSucceed_whenDifferentRoomSameTimeSlot() {
        // 這裡驗證的重點是:衝突查詢只針對「同一間會議室」,不同會議室不會互相干擾
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(eq(1L), any(), any(), any()))
                .thenReturn(List.of());  // 對「這間」會議室查詢,沒有衝突
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationMapper.toResponseDto(any(Reservation.class)))
                .thenReturn(ReservationResponseDto.builder().id(200L).build());

        ReservationResponseDto result = reservationService.create(validRequest);

        assertThat(result).isNotNull();
        verify(reservationRepository).findConflictingReservations(eq(1L), any(), any(), any());
    }

    @Test
    void reserveRoom_shouldSucceed_whenExistingReservationIsRejected() {
        // 驗證重點:findConflictingReservations 本身只會回傳「真的算佔用」的預約,
        // 這裡直接模擬「查詢結果是空的」,代表 rejected 那筆已經被 Repository 層的條件排除掉了
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationMapper.toResponseDto(any(Reservation.class)))
                .thenReturn(ReservationResponseDto.builder().id(201L).build());

        assertThatCode(() -> reservationService.create(validRequest))
                .doesNotThrowAnyException();
    }

    @Test
    void requestCancel_shouldSucceed_whenUserIsOwner() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .user(testUser)
                .status(ReservationStatus.APPROVED)
                .build();

        reservation.requestCancel(1L);  // testUser 的 id 就是 1L

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCEL_REQUESTED);
        assertThat(reservation.getPreviousStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void requestCancel_shouldThrowForbiddenException_whenUserIsNotOwner() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .user(testUser)  // owner 是 id=1 的使用者
                .status(ReservationStatus.APPROVED)
                .build();

        assertThatThrownBy(() -> reservation.requestCancel(999L))  // 冒充別人
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("只有預約本人");
    }

    @Test
    void requestCancel_shouldThrowException_whenStatusIsAlreadyRejected() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .user(testUser)
                .status(ReservationStatus.REJECTED)
                .build();

        assertThatThrownBy(() -> reservation.requestCancel(1L))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void review_shouldSucceed_whenReviewerHasReviewerRole() {
        User reviewer = User.builder().id(3L).role(Role.REVIEWER).build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.CANCEL_REQUESTED)
                .previousStatus(ReservationStatus.APPROVED)
                .build();

        ReviewRequestDto reviewRequest = new ReviewRequestDto();
        reviewRequest.setReviewerId(3L);
        reviewRequest.setAction(ReviewAction.APPROVED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(3L)).thenReturn(Optional.of(reviewer));
        when(reservationMapper.toResponseDto(any(Reservation.class)))
                .thenReturn(ReservationResponseDto.builder().id(1L).status(ReservationStatus.CANCELLED).build());

        ReservationResponseDto result = reservationService.review(1L, reviewRequest);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationReviewRepository).save(any(ReservationReview.class));
    }

    @Test
    void review_shouldThrowForbiddenException_whenReviewerHasUserRole() {
        User notReviewer = User.builder().id(1L).role(Role.USER).build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .status(ReservationStatus.CANCEL_REQUESTED)
                .build();

        ReviewRequestDto reviewRequest = new ReviewRequestDto();
        reviewRequest.setReviewerId(1L);
        reviewRequest.setAction(ReviewAction.APPROVED);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(userRepository.findById(1L)).thenReturn(Optional.of(notReviewer));

        assertThatThrownBy(() -> reservationService.review(1L, reviewRequest))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("REVIEWER");

        verify(reservationReviewRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldSucceed_whenExistingReservationIsCancelled() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationMapper.toResponseDto(any(Reservation.class)))
                .thenReturn(ReservationResponseDto.builder().id(202L).build());

        assertThatCode(() -> reservationService.create(validRequest))
                .doesNotThrowAnyException();
    }

    @Test
    void reserveRoom_shouldThrowConflictException_whenExistingReservationIsProcessing() {
        Reservation blockingReservation = mock(Reservation.class);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of(blockingReservation));

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(ReservationConflictException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveRoom_shouldThrowConflictException_whenExistingReservationIsApproved() {
        Reservation blockingReservation = mock(Reservation.class);

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findConflictingReservations(any(), any(), any(), any()))
                .thenReturn(List.of(blockingReservation));

        assertThatThrownBy(() -> reservationService.create(validRequest))
                .isInstanceOf(ReservationConflictException.class);

        verify(reservationRepository, never()).save(any());
    }
}

package com.company.meetingroom.service;

import com.company.meetingroom.dto.ReservationRequestDto;
import com.company.meetingroom.dto.ReservationResponseDto;
import com.company.meetingroom.entity.Reservation;
import com.company.meetingroom.entity.ReservationStatus;
import com.company.meetingroom.entity.Room;
import com.company.meetingroom.entity.User;
import com.company.meetingroom.exception.InvalidReservationException;
import com.company.meetingroom.exception.ReservationConflictException;
import com.company.meetingroom.exception.ResourceNotFoundException;
import com.company.meetingroom.mapper.ReservationMapper;
import com.company.meetingroom.repository.ReservationRepository;
import com.company.meetingroom.repository.RoomRepository;
import com.company.meetingroom.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.meetingroom.dto.CancelRequestDto;
import com.company.meetingroom.dto.ReviewRequestDto;
import com.company.meetingroom.entity.ReservationReview;
import com.company.meetingroom.entity.ReviewAction;
import com.company.meetingroom.entity.Role;
import com.company.meetingroom.exception.ForbiddenException;
import com.company.meetingroom.repository.ReservationReviewRepository;
import com.company.meetingroom.specification.ReservationSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED);

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;
    private final ReservationReviewRepository reservationReviewRepository;

    @Transactional
    public ReservationResponseDto create(ReservationRequestDto requestDto) {
        // 第 1 步:確認關聯資料真的存在
        Room room = roomRepository.findById(requestDto.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + requestDto.getRoomId() + " 的會議室"));

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + requestDto.getUserId() + " 的使用者"));

        // 第 2 步:驗證時間邏輯本身合不合理
        validateTimeRules(requestDto.getStartTime(), requestDto.getEndTime());

        // 第 3 步:驗證人數是否超過容量
        validateAttendeeCount(requestDto.getAttendeeCount(), room.getCapacity());

        // 第 4 步:檢查時段衝突(帶鎖查詢)
        checkForConflicts(room.getId(), requestDto.getStartTime(), requestDto.getEndTime());

        // 第 5 步:全部檢查通過,建立預約
        LocalDateTime now = LocalDateTime.now();
        Reservation reservation = Reservation.builder()
                .room(room)
                .user(user)
                .startTime(requestDto.getStartTime())
                .endTime(requestDto.getEndTime())
                .subject(requestDto.getSubject())
                .purpose(requestDto.getPurpose())
                .attendeeCount(requestDto.getAttendeeCount())
                .status(ReservationStatus.PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponseDto(saved);
    }

    @Transactional
    public ReservationResponseDto cancelRequest(Long reservationId, CancelRequestDto requestDto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + reservationId + " 的預約"));

        reservation.requestCancel(requestDto.getUserId());

        return reservationMapper.toResponseDto(reservation);
    }

    @Transactional
    public ReservationResponseDto review(Long reservationId, ReviewRequestDto requestDto) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + reservationId + " 的預約"));

        User reviewer = userRepository.findById(requestDto.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("找不到 id 為 " + requestDto.getReviewerId() + " 的使用者"));

        if (reviewer.getRole() != Role.REVIEWER && reviewer.getRole() != Role.ADMIN) {
            throw new ForbiddenException("只有 REVIEWER 或 ADMIN 可以審核");
        }

        if (reservation.getStatus() != ReservationStatus.CANCEL_REQUESTED) {
            throw new InvalidReservationException("此預約目前不是待審核狀態");
        }

        if (requestDto.getAction() == ReviewAction.APPROVED) {
            reservation.approveCancelRequest();
        } else {
            reservation.rejectCancelRequest();
        }

        LocalDateTime now = LocalDateTime.now();
        ReservationReview review = ReservationReview.builder()
                .reservation(reservation)
                .reviewer(reviewer)
                .action(requestDto.getAction())
                .comment(requestDto.getComment())
                .reviewedAt(now)
                .createdAt(now)
                .build();
        reservationReviewRepository.save(review);

        return reservationMapper.toResponseDto(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponseDto> search(
            LocalDate dateFrom, LocalDate dateTo, Long roomId, String roomName,
            String username, ReservationStatus status, Pageable pageable) {

        Specification<Reservation> spec = Specification
                .where(ReservationSpecifications.dateFrom(dateFrom))
                .and(ReservationSpecifications.dateTo(dateTo))
                .and(ReservationSpecifications.roomId(roomId))
                .and(ReservationSpecifications.roomName(roomName))
                .and(ReservationSpecifications.username(username))
                .and(ReservationSpecifications.status(status));

        return reservationRepository.findAll(spec, pageable)
                .map(reservationMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getByRoomId(Long roomId) {
        // 先確認會議室真的存在,不存在就回 404,而不是默默回傳空清單
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("找不到 id 為 " + roomId + " 的會議室");
        }

        return reservationRepository.findByRoomIdOrderByStartTimeAsc(roomId)
                .stream()
                .map(reservationMapper::toResponseDto)
                .toList();
    }

    private void validateTimeRules(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidReservationException("startTime 必須早於 endTime");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidReservationException("不可預約過去時間");
        }
        if (startTime.getMinute() % 30 != 0 || endTime.getMinute() % 30 != 0) {
            throw new InvalidReservationException("預約時間需以 30 分鐘為單位");
        }
    }

    private void validateAttendeeCount(Integer attendeeCount, Integer roomCapacity) {
        if (attendeeCount > roomCapacity) {
            throw new InvalidReservationException(
                    "出席人數 " + attendeeCount + " 超過會議室容量 " + roomCapacity);
        }
    }

    private void checkForConflicts(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                roomId, startTime, endTime, BLOCKING_STATUSES);

        if (!conflicts.isEmpty()) {
            throw new ReservationConflictException("此會議室在該時段已被預約");
        }
    }
}

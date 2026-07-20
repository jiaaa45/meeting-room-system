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

package com.company.meetingroom.repository;

import com.company.meetingroom.entity.*;
import com.company.meetingroom.repository.projection.RoomUsageProjection;
import com.company.meetingroom.repository.projection.StatusCountProjection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    private Room room;
    private User user;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        room = roomRepository.save(Room.builder()
                .name("測試專用會議室")
                .capacity(10)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        user = userRepository.save(User.builder()
                .username("測試專用使用者")
                .email("repo-test-" + System.nanoTime() + "@example.com")
                .role(Role.USER)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Reservation saveReservation(LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        Reservation reservation = Reservation.builder()
                .room(room)
                .user(user)
                .startTime(start)
                .endTime(end)
                .subject("測試預約")
                .attendeeCount(2)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return reservationRepository.save(reservation);
    }

    @Test
    void contextLoads() {
        assertThat(room).isNotNull();
        assertThat(user).isNotNull();
    }

    @Test
    void findConflictingReservations_shouldFindOverlappingReservation() {
        LocalDateTime existingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime existingEnd = existingStart.plusHours(2);  // 10:00 - 12:00
        saveReservation(existingStart, existingEnd, ReservationStatus.APPROVED);

        // 查詢 11:00 - 11:30,這個區間完全落在既有預約中間,應該要抓到衝突
        LocalDateTime queryStart = existingStart.plusHours(1);
        LocalDateTime queryEnd = queryStart.plusMinutes(30);

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                room.getId(), queryStart, queryEnd,
                List.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED));

        assertThat(conflicts).hasSize(1);
    }

    @Test
    void findConflictingReservations_shouldNotFindReservation_whenTimeDoesNotOverlap() {
        LocalDateTime existingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime existingEnd = existingStart.plusHours(1);  // 10:00 - 11:00
        saveReservation(existingStart, existingEnd, ReservationStatus.APPROVED);

        // 查詢 14:00 - 15:00,跟既有預約完全不相關
        LocalDateTime queryStart = existingStart.plusHours(4);
        LocalDateTime queryEnd = queryStart.plusHours(1);

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                room.getId(), queryStart, queryEnd,
                List.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED));

        assertThat(conflicts).isEmpty();
    }

    @Test
    void findConflictingReservations_shouldExcludeRejectedAndCancelledReservations() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        saveReservation(start, end, ReservationStatus.REJECTED);
        saveReservation(start, end, ReservationStatus.CANCELLED);

        // 同樣的時段,再查一次,傳入的 blockingStatuses 只包含 PROCESSING/APPROVED
        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                room.getId(), start, end,
                List.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED));

        // 雖然資料庫裡有兩筆同時段的預約,但因為狀態是 REJECTED/CANCELLED,
        // 不在 blockingStatuses 清單內,查詢應該完全找不到任何衝突
        assertThat(conflicts).isEmpty();
    }

    @Test
    void findConflictingReservations_shouldFindProcessingAndApprovedReservations() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        saveReservation(start, end, ReservationStatus.PROCESSING);

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                room.getId(), start, end,
                List.of(ReservationStatus.PROCESSING, ReservationStatus.APPROVED));

        // 這次故意驗證:PROCESSING 狀態真的會被抓出來當作衝突(對應考題情境 11)
        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getStatus()).isEqualTo(ReservationStatus.PROCESSING);
    }

    @Test
    void findByRoomIdOrderByStartTimeAsc_shouldReturnReservationsSortedByStartTime() {
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);

        saveReservation(base.plusHours(4), base.plusHours(5), ReservationStatus.APPROVED);  // 13:00
        saveReservation(base, base.plusHours(1), ReservationStatus.APPROVED);               // 09:00
        saveReservation(base.plusHours(2), base.plusHours(3), ReservationStatus.APPROVED);  // 11:00

        List<Reservation> results = reservationRepository.findByRoomIdOrderByStartTimeAsc(room.getId());

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getStartTime()).isEqualTo(base);              // 09:00 排第一
        assertThat(results.get(1).getStartTime()).isEqualTo(base.plusHours(2)); // 11:00 排第二
        assertThat(results.get(2).getStartTime()).isEqualTo(base.plusHours(4)); // 13:00 排第三
    }

    @Test
    void countByStatusInMonth_shouldGroupCorrectlyByStatus() {
        LocalDateTime monthStart = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        saveReservation(monthStart.plusDays(1), monthStart.plusDays(1).plusHours(1), ReservationStatus.APPROVED);
        saveReservation(monthStart.plusDays(2), monthStart.plusDays(2).plusHours(1), ReservationStatus.APPROVED);
        saveReservation(monthStart.plusDays(3), monthStart.plusDays(3).plusHours(1), ReservationStatus.PROCESSING);

        List<StatusCountProjection> results = reservationRepository.countByStatusInMonth(monthStart, monthEnd);

        long approvedCount = results.stream()
                .filter(r -> r.getStatus() == ReservationStatus.APPROVED)
                .findFirst()
                .map(StatusCountProjection::getCount)
                .orElse(0L);
        long processingCount = results.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PROCESSING)
                .findFirst()
                .map(StatusCountProjection::getCount)
                .orElse(0L);

        assertThat(approvedCount).isEqualTo(2L);
        assertThat(processingCount).isEqualTo(1L);
    }

    @Test
    void findTopUsedRooms_shouldCalculateReservationCountAndMinutesCorrectly() {
        LocalDateTime monthStart = LocalDateTime.of(2030, 1, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        // 這間會議室這個月有 2 筆 approved 預約,各 60 分鐘,加總應該是 120 分鐘
        saveReservation(monthStart.plusDays(1), monthStart.plusDays(1).plusHours(1), ReservationStatus.APPROVED);
        saveReservation(monthStart.plusDays(2), monthStart.plusDays(2).plusHours(1), ReservationStatus.APPROVED);
        // 這筆是 processing,不該被算進使用率統計裡
        saveReservation(monthStart.plusDays(3), monthStart.plusDays(3).plusHours(1), ReservationStatus.PROCESSING);

        List<RoomUsageProjection> results = reservationRepository.findTopUsedRooms(monthStart, monthEnd);

        assertThat(results).isNotEmpty();
        RoomUsageProjection thisRoomUsage = results.stream()
                .filter(r -> r.getRoomId().equals(room.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(thisRoomUsage.getReservationCount()).isEqualTo(2L);
        assertThat(thisRoomUsage.getTotalReservedMinutes()).isEqualTo(120L);
    }
}
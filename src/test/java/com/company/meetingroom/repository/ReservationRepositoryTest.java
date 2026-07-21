package com.company.meetingroom.repository;

import com.company.meetingroom.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;
import com.company.meetingroom.entity.ReservationStatus;

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
        room = roomRepository.findById(1L).orElseThrow();
        user = userRepository.findById(1L).orElseThrow();
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
}
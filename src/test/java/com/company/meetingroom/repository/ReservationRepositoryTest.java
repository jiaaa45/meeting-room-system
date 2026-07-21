package com.company.meetingroom.repository;

import com.company.meetingroom.entity.*;
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
        room = roomRepository.findById(1L).orElseThrow();
        user = userRepository.findById(1L).orElseThrow();
    }

    @Test
    void contextLoads() {
        assertThat(room).isNotNull();
        assertThat(user).isNotNull();
    }
}
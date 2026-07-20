package com.company.meetingroom.config;

import com.company.meetingroom.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataVerificationRunner implements CommandLineRunner {

    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        System.out.println("===== Room Repository 驗證開始 =====");
        roomRepository.findAll().forEach(room ->
            System.out.println(room.getId() + " | " + room.getName() + " | 容量:" + room.getCapacity())
        );
        System.out.println("===== 共 " + roomRepository.count() + " 筆會議室資料 =====");
    }
}
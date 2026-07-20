package com.company.meetingroom.mapper;

import com.company.meetingroom.dto.UserRequestDto;
import com.company.meetingroom.dto.UserResponseDto;
import com.company.meetingroom.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserMapper {

    public User toEntity(UserRequestDto dto) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .department(dto.getDepartment())
                .role(dto.getRole())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public UserResponseDto toResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .department(user.getDepartment())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
package com.company.meetingroom.dto;

import com.company.meetingroom.entity.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String department;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
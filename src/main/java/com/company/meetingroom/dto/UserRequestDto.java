package com.company.meetingroom.dto;

import com.company.meetingroom.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {

    @NotBlank(message = "使用者名稱不可為空")
    private String username;

    @NotBlank(message = "email 不可為空")
    @Email(message = "email 格式不正確")
    private String email;

    private String department;

    @NotNull(message = "角色不可為空")
    private Role role;
}

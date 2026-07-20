package com.company.meetingroom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelRequestDto {

    @NotNull(message = "userId 不可為空")
    private Long userId;

    private String reason;
}

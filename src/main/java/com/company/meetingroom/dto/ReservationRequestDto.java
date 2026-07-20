package com.company.meetingroom.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationRequestDto {

    @NotNull(message = "roomId 不可為空")
    private Long roomId;

    @NotNull(message = "userId 不可為空")
    private Long userId;

    @NotNull(message = "startTime 不可為空")
    private LocalDateTime startTime;

    @NotNull(message = "endTime 不可為空")
    private LocalDateTime endTime;

    @NotBlank(message = "subject 不可為空")
    private String subject;

    private String purpose;

    @NotNull(message = "attendeeCount 不可為空")
    @Positive(message = "attendeeCount 至少為 1")
    private Integer attendeeCount;
}
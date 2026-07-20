package com.company.meetingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomRequestDto {

    @NotBlank(message = "會議室名稱不可為空")
    private String name;

    @NotNull(message = "容量不可為空")
    @Positive(message = "容量必須大於 0")
    private Integer capacity;

    private String floor;

    private String location;
}

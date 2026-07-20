package com.company.meetingroom.dto;

import com.company.meetingroom.entity.ReviewAction;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDto {

    @NotNull(message = "reviewerId 不可為空")
    private Long reviewerId;

    @NotNull(message = "action 不可為空")
    private ReviewAction action;

    private String comment;
}

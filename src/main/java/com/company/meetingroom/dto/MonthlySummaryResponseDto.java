package com.company.meetingroom.dto;

import com.company.meetingroom.entity.ReservationStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MonthlySummaryResponseDto {
    private Integer year;
    private Integer month;
    private Map<ReservationStatus, Long> summary;
    private List<MonthlySummaryItemDto> items;
}

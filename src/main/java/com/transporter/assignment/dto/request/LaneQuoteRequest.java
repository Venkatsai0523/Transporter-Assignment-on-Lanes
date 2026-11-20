package com.transporter.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LaneQuoteRequest(
        @NotNull Long laneId,
        @NotNull @Positive BigDecimal quote
) {
}

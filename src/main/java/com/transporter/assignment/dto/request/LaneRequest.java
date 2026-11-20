package com.transporter.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LaneRequest(
        @NotNull Long id,
        @NotBlank String origin,
        @NotBlank String destination
) {
}

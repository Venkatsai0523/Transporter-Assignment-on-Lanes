package com.transporter.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignmentRequest(
        @NotNull @Positive Integer maxTransporters
) {
}

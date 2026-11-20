package com.transporter.assignment.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AssignmentResponse(
        String status,
        BigDecimal totalCost,
        List<LaneAssignmentResponse> assignments,
        List<Long> selectedTransporters
) {
}

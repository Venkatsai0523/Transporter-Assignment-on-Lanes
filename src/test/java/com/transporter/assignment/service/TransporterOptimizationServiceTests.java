package com.transporter.assignment.service;

import com.transporter.assignment.exception.NoFeasibleAssignmentException;
import com.transporter.assignment.exception.OptimizationLimitException;
import com.transporter.assignment.service.TransporterOptimizationService.LaneQuoteInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransporterOptimizationServiceTests {
    private final TransporterOptimizationService service = new TransporterOptimizationService();

    @Test
    void assignsOneLaneAndOneTransporter() {
        var result = optimize(List.of(1L), List.of(10L), 1,
                quote(1, 10, "500"));

        assertEquals(new BigDecimal("500"), result.totalCost());
        assertEquals(List.of(10L), result.selectedTransporters());
        assertEquals(10L, result.assignments().getFirst().transporterId());
    }

    @Test
    void minimizesCostAndMaximumTransporterUsage() {
        var result = optimize(List.of(1L, 2L), List.of(10L, 20L, 30L), 2,
                quote(1, 10, "100"), quote(1, 20, "500"),
                quote(2, 20, "100"), quote(2, 30, "500"));

        assertEquals(new BigDecimal("200"), result.totalCost());
        assertEquals(List.of(10L, 20L), result.selectedTransporters());
    }

    @Test
    void respectsMaximumTransporterLimit() {
        var result = optimize(List.of(1L, 2L), List.of(10L, 20L, 30L), 2,
                quote(1, 10, "100"), quote(1, 20, "300"), quote(1, 30, "300"),
                quote(2, 10, "100"), quote(2, 20, "300"), quote(2, 30, "300"));

        assertTrue(result.selectedTransporters().size() <= 2);
        assertEquals(new BigDecimal("200"), result.totalCost());
    }

    @Test
    void usesDeterministicTransporterTieBreak() {
        var result = optimize(List.of(1L), List.of(20L, 10L), 1,
                quote(1, 20, "100"), quote(1, 10, "100"));

        assertEquals(List.of(10L), result.selectedTransporters());
    }

    @Test
    void returnsNoFeasibleWhenLaneHasNoQuote() {
        assertThrows(NoFeasibleAssignmentException.class,
                () -> optimize(List.of(1L, 2L), List.of(10L), 1,
                        quote(1, 10, "100")));
    }

    @Test
    void selectedTransporterMustActuallyReceiveLane() {
        var result = optimize(List.of(1L, 2L), List.of(10L, 20L), 2,
                quote(1, 10, "100"), quote(1, 20, "150"),
                quote(2, 10, "100"), quote(2, 20, "150"));

        assertEquals(1, result.selectedTransporters().size());
    }

    @Test
    void handlesLargeMoneyWithoutOverflow() {
        var result = optimize(List.of(1L, 2L), List.of(10L), 1,
                quote(1, 10, "900000000000000000"), quote(2, 10, "900000000000000000"));

        assertEquals(new BigDecimal("1800000000000000000"), result.totalCost());
    }

    @Test
    void rejectsZeroAndNegativeMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> optimize(List.of(1L), List.of(10L), 0, quote(1, 10, "100")));
        assertThrows(IllegalArgumentException.class,
                () -> optimize(List.of(1L), List.of(10L), -1, quote(1, 10, "100")));
    }

    @Test
    void rejectsNegativeAndZeroQuotes() {
        assertThrows(IllegalArgumentException.class,
                () -> optimize(List.of(1L), List.of(10L), 1, quote(1, 10, "0")));
        assertThrows(IllegalArgumentException.class,
                () -> optimize(List.of(1L), List.of(10L), 1, quote(1, 10, "-1")));
    }

    @Test
    void rejectsUnsupportedInputSize() {
        assertThrows(OptimizationLimitException.class, () -> {
            var lanes = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();
            var transporters = java.util.stream.LongStream.rangeClosed(1, 21).boxed().toList();
            service.optimize(lanes, transporters, List.of(), 10);
        });
    }

    private TransporterOptimizationService.OptimizationResult optimize(
            List<Long> lanes,
            List<Long> transporters,
            int max,
            LaneQuoteInput... quotes
    ) {
        return service.optimize(lanes, transporters, List.of(quotes), max);
    }

    private LaneQuoteInput quote(long laneId, long transporterId, String amount) {
        return new LaneQuoteInput(laneId, transporterId, new BigDecimal(amount));
    }
}

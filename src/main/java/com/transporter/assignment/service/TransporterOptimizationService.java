package com.transporter.assignment.service;

import com.transporter.assignment.exception.NoFeasibleAssignmentException;
import com.transporter.assignment.exception.OptimizationLimitException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class TransporterOptimizationService {
    private static final int MAX_TRANSPORTERS = 20;
    private static final int MAX_LANES = 100;
    private static final long MAX_COMBINATIONS = 500_000;

    public OptimizationResult optimize(
            List<Long> laneIds,
            List<Long> transporterIds,
            List<LaneQuoteInput> quotes,
            int maxTransporters
    ) {
        if (laneIds == null || laneIds.isEmpty()) {
            throw new IllegalArgumentException("At least one lane is required.");
        }
        if (transporterIds == null || transporterIds.isEmpty()) {
            throw new IllegalArgumentException("At least one transporter is required.");
        }
        if (maxTransporters <= 0) {
            throw new IllegalArgumentException("maxTransporters must be greater than zero.");
        }
        if (laneIds.size() > MAX_LANES || transporterIds.size() > MAX_TRANSPORTERS) {
            throw new OptimizationLimitException(
                    "Input exceeds the supported exhaustive-search limit: maximum 100 lanes and 20 transporters.");
        }

        List<Long> sortedLanes = laneIds.stream().distinct().sorted().toList();
        List<Long> sortedTransporters = transporterIds.stream().distinct().sorted().toList();
        if (sortedLanes.size() != laneIds.size() || sortedTransporters.size() != transporterIds.size()) {
            throw new IllegalArgumentException("Lane and transporter IDs must be unique.");
        }

        Map<Long, Map<Long, BigDecimal>> quoteByLane = new HashMap<>();
        for (LaneQuoteInput quote : quotes) {
            if (!sortedLanes.contains(quote.laneId()) || !sortedTransporters.contains(quote.transporterId())) {
                throw new IllegalArgumentException("Every quotation must reference an existing lane and transporter.");
            }
            if (quote.quote() == null || quote.quote().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quotation values must be greater than zero.");
            }
            Map<Long, BigDecimal> laneQuotes = quoteByLane.computeIfAbsent(quote.laneId(), ignored -> new HashMap<>());
            laneQuotes.merge(quote.transporterId(), quote.quote(), (existing, replacement) ->
                    existing.compareTo(replacement) <= 0 ? existing : replacement);
        }

        int effectiveLimit = Math.min(maxTransporters, sortedTransporters.size());
        long combinations = combinationCount(sortedTransporters.size(), effectiveLimit);
        if (combinations > MAX_COMBINATIONS) {
            throw new OptimizationLimitException(
                    "Input exceeds the supported exhaustive-search limit of " + MAX_COMBINATIONS
                            + " transporter combinations.");
        }

        OptimizationResult best = null;
        for (int size = effectiveLimit; size >= 1; size--) {
            if (size > sortedTransporters.size()) {
                continue;
            }
            OptimizationResult bestAtSize = evaluateCombinations(sortedLanes, sortedTransporters, quoteByLane, size);
            if (bestAtSize != null) {
                best = bestAtSize;
                break;
            }
        }

        if (best == null) {
            throw new NoFeasibleAssignmentException(
                    "No valid assignment exists: every lane must have at least one eligible transporter.");
        }
        return best;
    }

    private OptimizationResult evaluateCombinations(
            List<Long> laneIds,
            List<Long> transporterIds,
            Map<Long, Map<Long, BigDecimal>> quoteByLane,
            int size
    ) {
        int[] combination = new int[size];
        OptimizationResult bestForSize = null;
        return generateCombinations(
                transporterIds,
                laneIds,
                quoteByLane,
                combination,
                size,
                0,
                0,
                bestForSize);
    }

    private OptimizationResult generateCombinations(
            List<Long> transporterIds,
            List<Long> laneIds,
            Map<Long, Map<Long, BigDecimal>> quoteByLane,
            int[] combination,
            int size,
            int nextIndex,
            int position,
            OptimizationResult currentBest
    ) {
        if (position == size) {
            List<Long> selected = new ArrayList<>(size);
            for (int index : combination) {
                selected.add(transporterIds.get(index));
            }
            OptimizationResult candidate = evaluateOneCombination(laneIds, selected, quoteByLane);
            return candidate != null && isBetter(candidate, currentBest) ? candidate : currentBest;
        }

        for (int index = nextIndex; index <= transporterIds.size() - (size - position); index++) {
            combination[position] = index;
            currentBest = generateCombinations(
                    transporterIds,
                    laneIds,
                    quoteByLane,
                    combination,
                    size,
                    index + 1,
                    position + 1,
                    currentBest);
        }
        return currentBest;
    }

    private OptimizationResult evaluateOneCombination(
            List<Long> laneIds,
            List<Long> selected,
            Map<Long, Map<Long, BigDecimal>> quoteByLane
    ) {
        Set<Long> selectedSet = new HashSet<>(selected);
        Set<Long> used = new HashSet<>();
        List<LaneAssignment> assignments = new ArrayList<>(laneIds.size());
        BigDecimal total = BigDecimal.ZERO;

        for (Long laneId : laneIds) {
            Map<Long, BigDecimal> laneQuotes = quoteByLane.get(laneId);
            if (laneQuotes == null) {
                return null;
            }

            Long chosenTransporter = null;
            BigDecimal chosenQuote = null;
            for (Long transporterId : selected) {
                BigDecimal quote = laneQuotes.get(transporterId);
                if (quote == null) {
                    continue;
                }
                if (chosenQuote == null
                        || quote.compareTo(chosenQuote) < 0
                        || (quote.compareTo(chosenQuote) == 0
                        && transporterId < Objects.requireNonNull(chosenTransporter))) {
                    chosenQuote = quote;
                    chosenTransporter = transporterId;
                }
            }
            if (chosenTransporter == null) {
                return null;
            }
            used.add(chosenTransporter);
            total = total.add(chosenQuote);
            assignments.add(new LaneAssignment(laneId, chosenTransporter));
        }

        if (!used.equals(selectedSet)) {
            return null;
        }
        return new OptimizationResult(total, List.copyOf(assignments), List.copyOf(selected));
    }

    private boolean isBetter(OptimizationResult candidate, OptimizationResult incumbent) {
        if (incumbent == null) {
            return true;
        }
        int costComparison = candidate.totalCost().compareTo(incumbent.totalCost());
        if (costComparison != 0) {
            return costComparison < 0;
        }
        int selectedComparison = compareIds(candidate.selectedTransporters(), incumbent.selectedTransporters());
        if (selectedComparison != 0) {
            return selectedComparison < 0;
        }
        return compareAssignments(candidate.assignments(), incumbent.assignments()) < 0;
    }

    private int compareIds(List<Long> left, List<Long> right) {
        int limit = Math.min(left.size(), right.size());
        for (int i = 0; i < limit; i++) {
            int comparison = left.get(i).compareTo(right.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private int compareAssignments(List<LaneAssignment> left, List<LaneAssignment> right) {
        int limit = Math.min(left.size(), right.size());
        for (int i = 0; i < limit; i++) {
            int laneComparison = Long.compare(left.get(i).laneId(), right.get(i).laneId());
            if (laneComparison == 0) {
                laneComparison = Long.compare(
                        left.get(i).transporterId(), right.get(i).transporterId());
            }
            if (laneComparison != 0) {
                return laneComparison;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private long combinationCount(int n, int r) {
        long total = 0;
        for (int size = 1; size <= r; size++) {
            total = saturatingAdd(total, binomial(n, size));
            if (total > MAX_COMBINATIONS) {
                return total;
            }
        }
        return total;
    }

    private long binomial(int n, int k) {
        k = Math.min(k, n - k);
        long result = 1;
        for (int i = 1; i <= k; i++) {
            if (result > MAX_COMBINATIONS / (n - k + i)) {
                return MAX_COMBINATIONS + 1;
            }
            result = result * (n - k + i) / i;
        }
        return result;
    }

    private long saturatingAdd(long left, long right) {
        if (right > MAX_COMBINATIONS - left) {
            return MAX_COMBINATIONS + 1;
        }
        return left + right;
    }

    public record LaneQuoteInput(long laneId, long transporterId, BigDecimal quote) {
    }

    public record LaneAssignment(long laneId, long transporterId) {
    }

    public record OptimizationResult(
            BigDecimal totalCost,
            List<LaneAssignment> assignments,
            List<Long> selectedTransporters
    ) {
    }
}

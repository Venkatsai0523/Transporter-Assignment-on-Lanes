package com.transporter.assignment.service;

import com.transporter.assignment.dto.request.InputRequest;
import com.transporter.assignment.dto.request.LaneQuoteRequest;
import com.transporter.assignment.dto.request.LaneRequest;
import com.transporter.assignment.dto.request.TransporterRequest;
import com.transporter.assignment.entity.Lane;
import com.transporter.assignment.entity.LaneQuote;
import com.transporter.assignment.entity.Transporter;
import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.exception.ResourceNotFoundException;
import com.transporter.assignment.repository.LaneQuoteRepository;
import com.transporter.assignment.repository.LaneRepository;
import com.transporter.assignment.repository.TransporterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TransporterService {
    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;
    private final TransporterOptimizationService optimizationService;

    public TransporterService(
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository laneQuoteRepository,
            TransporterOptimizationService optimizationService
    ) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.laneQuoteRepository = laneQuoteRepository;
        this.optimizationService = optimizationService;
    }

    @Transactional
    public void replaceInput(InputRequest request) {
        validateInput(request);

        laneQuoteRepository.deleteAll();
        laneQuoteRepository.flush();
        laneRepository.deleteAll();
        laneRepository.flush();
        transporterRepository.deleteAll();
        transporterRepository.flush();

        Map<Long, Lane> lanes = new LinkedHashMap<>();
        for (LaneRequest laneRequest : request.lanes()) {
            Lane lane = new Lane(laneRequest.id(), laneRequest.origin().trim(), laneRequest.destination().trim());
            lanes.put(laneRequest.id(), laneRepository.save(lane));
        }

        Map<Long, Transporter> transporters = new LinkedHashMap<>();
        for (TransporterRequest transporterRequest : request.transporters()) {
            Transporter transporter =
                    new Transporter(transporterRequest.id(), transporterRequest.name().trim());
            transporters.put(transporterRequest.id(), transporterRepository.save(transporter));
        }

        List<LaneQuote> quotes = new ArrayList<>();
        for (TransporterRequest transporterRequest : request.transporters()) {
            Transporter transporter = transporters.get(transporterRequest.id());
            for (LaneQuoteRequest quoteRequest : transporterRequest.laneQuotes()) {
                Lane lane = lanes.get(quoteRequest.laneId());
                quotes.add(new LaneQuote(lane, transporter, quoteRequest.quote()));
            }
        }
        laneQuoteRepository.saveAll(quotes);
    }

    @Transactional(readOnly = true)
    public TransporterOptimizationService.OptimizationResult optimize(int maxTransporters) {
        List<Lane> lanes = laneRepository.findAll();
        List<Transporter> transporters = transporterRepository.findAll();
        if (lanes.isEmpty() || transporters.isEmpty()) {
            throw new ResourceNotFoundException("No input dataset has been submitted.");
        }

        SortedSet<Long> sortedLaneIds = new TreeSet<>();
        for (Lane lane : lanes) {
            sortedLaneIds.add(Objects.requireNonNull(lane.getId(), "Persisted lane ID must not be null."));
        }
        SortedSet<Long> sortedTransporterIds = new TreeSet<>();
        for (Transporter transporter : transporters) {
            sortedTransporterIds.add(
                    Objects.requireNonNull(transporter.getId(), "Persisted transporter ID must not be null."));
        }
        List<Long> laneIds = List.copyOf(sortedLaneIds);
        List<Long> transporterIds = List.copyOf(sortedTransporterIds);
        List<TransporterOptimizationService.LaneQuoteInput> quotes = new ArrayList<>();
        for (Lane lane : lanes) {
            for (LaneQuote quote : lane.getQuotes()) {
                quotes.add(new TransporterOptimizationService.LaneQuoteInput(
                        lane.getId(),
                        quote.getTransporter().getId(),
                        quote.getQuote()));
            }
        }

        return optimizationService.optimize(laneIds, transporterIds, quotes, maxTransporters);
    }

    private void validateInput(InputRequest request) {
        if (request == null) {
            throw new InvalidInputException("Request body is required.");
        }
        if (request.lanes() == null || request.lanes().isEmpty()) {
            throw new InvalidInputException("At least one lane is required.");
        }
        if (request.transporters() == null || request.transporters().isEmpty()) {
            throw new InvalidInputException("At least one transporter is required.");
        }

        Set<Long> laneIds = new HashSet<>();
        for (LaneRequest lane : request.lanes()) {
            if (lane.id() == null || lane.id() <= 0) {
                throw new InvalidInputException("Lane IDs must be positive.");
            }
            if (!laneIds.add(lane.id())) {
                throw new InvalidInputException("Duplicate lane ID: " + lane.id());
            }
        }

        Set<Long> transporterIds = new HashSet<>();
        for (TransporterRequest transporter : request.transporters()) {
            if (transporter.id() == null || transporter.id() <= 0) {
                throw new InvalidInputException("Transporter IDs must be positive.");
            }
            if (!transporterIds.add(transporter.id())) {
                throw new InvalidInputException("Duplicate transporter ID: " + transporter.id());
            }
            if (transporter.laneQuotes() == null) {
                throw new InvalidInputException("laneQuotes is required for transporter " + transporter.id());
            }

            Set<Long> quotedLaneIds = new HashSet<>();
            for (LaneQuoteRequest quote : transporter.laneQuotes()) {
                if (quote.laneId() == null || !laneIds.contains(quote.laneId())) {
                    throw new InvalidInputException(
                            "Quotation references unknown lane ID: " + quote.laneId());
                }
                if (!quotedLaneIds.add(quote.laneId())) {
                    throw new InvalidInputException(
                            "Duplicate quotation for lane " + quote.laneId()
                                    + " from transporter " + transporter.id());
                }
                if (quote.quote() == null || quote.quote().signum() <= 0) {
                    throw new InvalidInputException(
                            "Quotation must be greater than zero for lane " + quote.laneId());
                }
            }
        }
    }
}

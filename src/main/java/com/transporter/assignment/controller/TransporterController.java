package com.transporter.assignment.controller;

import com.transporter.assignment.dto.request.AssignmentRequest;
import com.transporter.assignment.dto.request.InputRequest;
import com.transporter.assignment.dto.response.AssignmentResponse;
import com.transporter.assignment.dto.response.InputResponse;
import com.transporter.assignment.dto.response.LaneAssignmentResponse;
import com.transporter.assignment.service.TransporterOptimizationService;
import com.transporter.assignment.service.TransporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporter Assignment", description = "Lane quotation submission and optimized assignment APIs")
public class TransporterController {
    private final TransporterService transporterService;

    public TransporterController(TransporterService transporterService) {
        this.transporterService = transporterService;
    }

    @GetMapping("/")
    @Operation(summary = "Open the Swagger UI")
    public RedirectView index() {
        return new RedirectView("/swagger-ui.html");
    }

    @PostMapping("/input")
    @Operation(summary = "Replace the active lane and quotation dataset")
    @ApiResponse(responseCode = "201", description = "Input saved")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    public ResponseEntity<InputResponse> submitInput(@Valid @RequestBody InputRequest request) {
        transporterService.replaceInput(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new InputResponse("success", "Input data saved successfully."));
    }

    @PostMapping("/assignment")
    @Operation(summary = "Calculate the lexicographically optimal transporter assignment")
    @ApiResponse(responseCode = "200", description = "Assignment calculated")
    @ApiResponse(responseCode = "404", description = "No input dataset found")
    @ApiResponse(responseCode = "422", description = "No feasible assignment")
    public AssignmentResponse getAssignment(@Valid @RequestBody AssignmentRequest request) {
        TransporterOptimizationService.OptimizationResult result =
                transporterService.optimize(request.maxTransporters());
        return new AssignmentResponse(
                "success",
                result.totalCost(),
                result.assignments().stream()
                        .map(assignment -> new LaneAssignmentResponse(
                                assignment.laneId(), assignment.transporterId()))
                        .toList(),
                result.selectedTransporters());
    }
}

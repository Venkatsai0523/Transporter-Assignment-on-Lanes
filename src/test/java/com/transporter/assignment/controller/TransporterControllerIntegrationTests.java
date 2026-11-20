package com.transporter.assignment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:workflow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class TransporterControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void submitFreshData() throws Exception {
        String input = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                    {"id": 2, "origin": "Delhi", "destination": "Bangalore"},
                    {"id": 3, "origin": "Chennai", "destination": "Kolkata"},
                    {"id": 4, "origin": "Pune", "destination": "Hyderabad"},
                    {"id": 5, "origin": "Ahmedabad", "destination": "Jaipur"}
                  ],
                  "transporters": [
                    {"id": 1, "name": "Transporter T1", "laneQuotes": [
                      {"laneId": 1, "quote": 5000}, {"laneId": 2, "quote": 6000}]},
                    {"id": 2, "name": "Transporter T2", "laneQuotes": [
                      {"laneId": 3, "quote": 7000}, {"laneId": 4, "quote": 4000}]},
                    {"id": 3, "name": "Transporter T3", "laneQuotes": [
                      {"laneId": 5, "quote": 3000}]}
                  ]
                }
                """;
        mockMvc.perform(post("/api/v1/transporters/input")
                        .header("Content-Type", "application/json")
                        .content(input))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void completeWorkflowReturnsAssignment() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .header("Content-Type", "application/json")
                        .content("{\"maxTransporters\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCost").value(25000))
                .andExpect(jsonPath("$.assignments.length()").value(5))
                .andExpect(jsonPath("$.selectedTransporters.length()").value(3));
    }

    @Test
    void repeatedInputReplacesExistingData() throws Exception {
        String replacement = """
                {
                  "lanes": [{"id": 9, "origin": "A", "destination": "B"}],
                  "transporters": [{"id": 8, "name": "T8", "laneQuotes": [
                    {"laneId": 9, "quote": 123}
                  ]}]
                }
                """;
        mockMvc.perform(post("/api/v1/transporters/input")
                        .header("Content-Type", "application/json")
                        .content(replacement))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .header("Content-Type", "application/json")
                        .content("{\"maxTransporters\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(123))
                .andExpect(jsonPath("$.assignments[0].laneId").value(9))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(8));
    }

    @Test
    void invalidInputReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/input")
                        .header("Content-Type", "application/json")
                        .content("{\"lanes\":[],\"transporters\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}

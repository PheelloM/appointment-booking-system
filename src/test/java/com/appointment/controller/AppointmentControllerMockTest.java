package com.appointment.controller;

import com.appointment.dto.AppointmentRequest;
import com.appointment.dto.AppointmentResponse;
import com.appointment.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AppointmentControllerMockTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    private ObjectMapper objectMapper;
    private AppointmentController appointmentController;

    // Constants for test data
    private static final String VALID_BOOKING_REFERENCE = "APT-TEST-123";
    private static final String INVALID_BOOKING_REFERENCE = "INVALID-REF";
    private static final String CUSTOMER_NAME = "Test User";
    private static final String CUSTOMER_EMAIL = "test@example.com";
    private static final String CUSTOMER_PHONE = "555-123-4567";
    private static final Long BRANCH_ID = 1L;
    private static final String BRANCH_NAME = "Test Branch";
    private static final String BRANCH_ADDRESS = "123 Test St";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Configure ObjectMapper with JavaTimeModule
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        appointmentController = new AppointmentController(appointmentService);

        // Setup MockMvc with standalone configuration
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .defaultRequest(post("/").contentType(MediaType.APPLICATION_JSON))
                .build();
    }

    // CREATE APPOINTMENT TESTS

    @Test
    @WithMockUser(authorities = "SCOPE_appointment:write")
    void createAppointment_WithValidRequest_ShouldReturnAppointment() throws Exception {
        // Given
        AppointmentRequest request = createValidAppointmentRequest();
        AppointmentResponse mockResponse = createMockAppointmentResponse();

        when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/appointments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.customerName", is(CUSTOMER_NAME)))
                .andExpect(jsonPath("$.customerEmail", is(CUSTOMER_EMAIL)))
                .andExpect(jsonPath("$.bookingReference", is(VALID_BOOKING_REFERENCE)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.branchName", is(BRANCH_NAME)))
                .andExpect(jsonPath("$.branchAddress", is(BRANCH_ADDRESS)));

        verify(appointmentService).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_appointment:write")
    void createAppointment_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing required fields
        AppointmentRequest invalidRequest = new AppointmentRequest();
        // Missing customerName, email, branchId, etc.

        // When & Then
        mockMvc.perform(post("/api/appointments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).createAppointment(any(AppointmentRequest.class));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_appointment:write")
    void createAppointment_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid email format
        AppointmentRequest invalidRequest = createValidAppointmentRequest();
        invalidRequest.setCustomerEmail("invalid-email");

        // When & Then
        mockMvc.perform(post("/api/appointments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).createAppointment(any(AppointmentRequest.class));
    }

    // GET APPOINTMENT TESTS

    @Test
    @WithMockUser(authorities = "SCOPE_appointment:read")
    void getAppointment_WithValidReference_ShouldReturnAppointment() throws Exception {
        // Given
        AppointmentResponse mockResponse = createMockAppointmentResponse();

        when(appointmentService.getAppointmentByReference(VALID_BOOKING_REFERENCE))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/appointments/{bookingReference}", VALID_BOOKING_REFERENCE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference", is(VALID_BOOKING_REFERENCE)))
                .andExpect(jsonPath("$.customerName", is(CUSTOMER_NAME)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(appointmentService).getAppointmentByReference(VALID_BOOKING_REFERENCE);
    }

    // CANCEL APPOINTMENT TESTS

    @Test
    @WithMockUser(authorities = "SCOPE_appointment:write")
    void cancelAppointment_WithValidReference_ShouldReturnOk() throws Exception {
        // Given
        doNothing().when(appointmentService).cancelAppointment(VALID_BOOKING_REFERENCE);

        // When & Then
        mockMvc.perform(delete("/api/appointments/{bookingReference}", VALID_BOOKING_REFERENCE)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(appointmentService).cancelAppointment(VALID_BOOKING_REFERENCE);
    }

    // Helper methods
    private AppointmentRequest createValidAppointmentRequest() {
        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName(CUSTOMER_NAME);
        request.setCustomerEmail(CUSTOMER_EMAIL);
        request.setCustomerPhone(CUSTOMER_PHONE);
        request.setBranchId(BRANCH_ID);
        request.setAppointmentDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(10, 0));
        return request;
    }

    private AppointmentResponse createMockAppointmentResponse() {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(1L);
        response.setCustomerName(CUSTOMER_NAME);
        response.setCustomerEmail(CUSTOMER_EMAIL);
        response.setCustomerPhone(CUSTOMER_PHONE);
        response.setBookingReference(VALID_BOOKING_REFERENCE);
        response.setStatus("CONFIRMED");
        response.setAppointmentDate(LocalDate.now().plusDays(1));
        response.setStartTime(LocalTime.of(10, 0));
        response.setEndTime(LocalTime.of(10, 30));
        response.setBranchName(BRANCH_NAME);
        response.setBranchAddress(BRANCH_ADDRESS);
        return response;
    }
}
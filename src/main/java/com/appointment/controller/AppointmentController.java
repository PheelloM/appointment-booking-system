package com.appointment.controller;

import com.appointment.dto.AppointmentRequest;
import com.appointment.dto.AppointmentResponse;
import com.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Changed from scope to role
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookingReference}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Changed from scope to role
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable String bookingReference) {
        AppointmentResponse response = appointmentService.getAppointmentByReference(bookingReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication authentication) {
        // Get the currently authenticated user's email
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String customerUser = userDetails.getUsername();

        List<AppointmentResponse> appointments = appointmentService.getAppointmentsByUsername(customerUser);
        return ResponseEntity.ok(appointments);
    }

    @DeleteMapping("/{bookingReference}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Changed from scope to role
    public ResponseEntity<Void> cancelAppointment(@PathVariable String bookingReference) {
        appointmentService.cancelAppointment(bookingReference);
        return ResponseEntity.ok().build();
    }
}
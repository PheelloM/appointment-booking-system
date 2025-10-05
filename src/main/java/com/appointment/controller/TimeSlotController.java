package com.appointment.controller;

import com.appointment.dto.TimeSlotDTO;
import com.appointment.service.TimeSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timeslots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeSlotDTO>> getAvailableTimeSlots(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TimeSlotDTO> availableSlots = timeSlotService.getAvailableTimeSlots(branchId, date);
        return ResponseEntity.ok(availableSlots);
    }
}
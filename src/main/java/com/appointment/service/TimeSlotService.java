package com.appointment.service;

import com.appointment.dto.TimeSlotDTO;
import com.appointment.entity.TimeSlot;
import com.appointment.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<TimeSlotDTO> getAvailableTimeSlots(Long branchId, LocalDate date) {
        List<TimeSlot> timeSlots = timeSlotRepository.findByBranchIdAndSlotDateAndAvailableTrue(branchId, date);

        return timeSlots.stream()
                .map(timeSlot -> new TimeSlotDTO(
                        timeSlot.getId(),
                        timeSlot.getBranch().getId(),
                        timeSlot.getBranch().getName(),
                        timeSlot.getSlotDate(),
                        timeSlot.getStartTime(),
                        timeSlot.getEndTime(),
                        timeSlot.getCapacity(),
                        timeSlot.getBookedCount(),
                        timeSlot.getAvailable()
                ))
                .collect(Collectors.toList());
    }
}
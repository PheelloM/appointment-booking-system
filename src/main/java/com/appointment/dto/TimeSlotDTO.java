package com.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class TimeSlotDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer bookedCount;
    private Boolean available;

    // Constructors
    public TimeSlotDTO() {}

    public TimeSlotDTO(Long id, Long branchId, String branchName, LocalDate slotDate,
                       LocalTime startTime, LocalTime endTime, Integer capacity,
                       Integer bookedCount, Boolean available) {
        this.id = id;
        this.branchId = branchId;
        this.branchName = branchName;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.bookedCount = bookedCount;
        this.available = available;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getBookedCount() { return bookedCount; }
    public void setBookedCount(Integer bookedCount) { this.bookedCount = bookedCount; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
}
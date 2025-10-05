package com.appointment.repository;

import com.appointment.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByBranchIdAndSlotDateAndAvailableTrue(Long branchId, LocalDate slotDate);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.branch.id = :branchId AND ts.slotDate = :slotDate " +
            "AND ts.startTime = :startTime AND ts.available = true")
    Optional<TimeSlot> findAvailableSlot(@Param("branchId") Long branchId,
                                         @Param("slotDate") LocalDate slotDate,
                                         @Param("startTime") LocalTime startTime);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.timeSlot.id = :timeSlotId " +
            "AND a.customerEmail = :customerEmail AND a.status = 'CONFIRMED'")
    boolean existsByTimeSlotAndCustomerEmail(@Param("timeSlotId") Long timeSlotId,
                                             @Param("customerEmail") String customerEmail);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.branch.id = :branchId AND ts.slotDate >= :startDate " +
            "AND ts.slotDate <= :endDate ORDER BY ts.slotDate, ts.startTime")
    List<TimeSlot> findSlotsByBranchAndDateRange(@Param("branchId") Long branchId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    List<TimeSlot> findByBranchIdAndSlotDate(Long branchId, LocalDate slotDate);
}

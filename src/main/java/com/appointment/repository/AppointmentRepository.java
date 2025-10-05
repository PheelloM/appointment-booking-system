package com.appointment.repository;

import com.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByBookingReference(String bookingReference);

    @Query("SELECT a FROM Appointment a WHERE a.customerName = :username ORDER BY a.timeSlot.slotDate ASC, a.timeSlot.startTime ASC")
    List<Appointment> findByUsernameOrderBySlotDateAndStartTime(@Param("username") String username);

    List<Appointment> findByCustomerEmailOrderByTimeSlotSlotDateAscTimeSlotStartTimeAsc(String customerEmail);

    List<Appointment> findByCustomerEmail(String customerEmail);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.timeSlot.id = :timeSlotId AND a.status != 'CANCELLED'")
    int countConfirmedAppointmentsByTimeSlot(@Param("timeSlotId") Long timeSlotId);

    @Query("SELECT a FROM Appointment a JOIN a.timeSlot ts WHERE ts.branch.id = :branchId AND a.status = 'CONFIRMED'")
    List<Appointment> findConfirmedAppointmentsByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.timeSlot.id = :timeSlotId " +
            "AND a.customerEmail = :customerEmail AND a.status = 'CONFIRMED'")
    boolean existsByTimeSlotIdAndCustomerEmail(@Param("timeSlotId") Long timeSlotId,
                                               @Param("customerEmail") String customerEmail);
}
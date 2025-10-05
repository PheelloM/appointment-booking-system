package com.appointment.service;

import com.appointment.dto.AppointmentRequest;
import com.appointment.dto.AppointmentResponse;
import com.appointment.entity.Appointment;
import com.appointment.entity.TimeSlot;
import com.appointment.exception.DoubleBookingException;
import com.appointment.exception.ResourceNotFoundException;
import com.appointment.exception.SlotNotAvailableException;
import com.appointment.repository.AppointmentRepository;
import com.appointment.repository.TimeSlotRepository;
import com.appointment.util.BookingReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final EmailService emailService;
    private final BookingReferenceGenerator bookingReferenceGenerator;

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    // Constructor injection
    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              EmailService emailService,
                              BookingReferenceGenerator bookingReferenceGenerator) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.emailService = emailService;
        this.bookingReferenceGenerator = bookingReferenceGenerator;
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Starting appointment creation for customer: {} at branch: {} on {} at {}",
                request.getCustomerEmail(), request.getBranchId(),
                request.getAppointmentDate(), request.getStartTime());

        // Check for existing appointment for same customer and time
        TimeSlot timeSlot = timeSlotRepository.findAvailableSlot(
                request.getBranchId(),
                request.getAppointmentDate(),
                request.getStartTime()
        ).orElseThrow(() -> new SlotNotAvailableException("Time slot not available"));

        log.debug("Found available time slot: {} for branch: {}", timeSlot.getId(), request.getBranchId());

        // Check if customer already has appointment for this slot
        boolean existingAppointment = timeSlotRepository.existsByTimeSlotAndCustomerEmail(
                timeSlot.getId(), request.getCustomerEmail());

        if (existingAppointment) {
            throw new DoubleBookingException("Customer already has an appointment for this time slot");
        }

        // Check capacity - if already full, mark as unavailable and throw exception
        int currentBookedCount = appointmentRepository.countConfirmedAppointmentsByTimeSlot(timeSlot.getId());
        log.debug("Current booked count for time slot {}: {}/{}",
                timeSlot.getId(), currentBookedCount, timeSlot.getCapacity());
        if (currentBookedCount >= timeSlot.getCapacity()) {
            log.warn("Time slot {} is fully booked. Capacity: {}, Current: {}",
                    timeSlot.getId(), timeSlot.getCapacity(), currentBookedCount);
            // Only save if we need to mark it as unavailable (might already be false)
            if (timeSlot.getAvailable()) {
                timeSlot.setAvailable(false);
                timeSlotRepository.save(timeSlot);
                log.info("Marked time slot {} as unavailable due to full capacity", timeSlot.getId());
            }
            throw new SlotNotAvailableException("Time slot is fully booked");
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setCustomerName(request.getCustomerName());
        appointment.setCustomerEmail(request.getCustomerEmail());
        appointment.setCustomerPhone(request.getCustomerPhone());
        appointment.setTimeSlot(timeSlot);
        String bookingReference = bookingReferenceGenerator.generateBookingReference();
        appointment.setBookingReference(bookingReference);

        log.info("Creating new appointment with reference: {} for customer: {}",
                bookingReference, request.getCustomerEmail());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.debug("Appointment saved successfully with ID: {}", savedAppointment.getId());

        // Update booked count and check if slot becomes full
        int newBookedCount = currentBookedCount + 1;
        timeSlot.setBookedCount(newBookedCount);
        log.debug("Updated booked count for time slot {}: {}/{}",
                timeSlot.getId(), newBookedCount, timeSlot.getCapacity());

        // If slot becomes full after this booking, mark it as unavailable
        if (newBookedCount >= timeSlot.getCapacity()) {
            timeSlot.setAvailable(false);
            log.info("Time slot {} is now fully booked. Marking as unavailable.", timeSlot.getId());
        }
        // Always save the timeSlot to update bookedCount and potentially availability
        timeSlotRepository.save(timeSlot);
        log.debug("Time slot {} updated successfully", timeSlot.getId());

        // Send confirmation email
        emailService.sendAppointmentConfirmation(savedAppointment);

        log.info("Appointment created successfully. Reference: {}, Customer: {}",
                bookingReference, request.getCustomerEmail());

        return mapToResponse(savedAppointment);
    }

    public AppointmentResponse getAppointmentByReference(String bookingReference) {
        log.debug("Fetching appointment by reference: {}", bookingReference);
        Appointment appointment = appointmentRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        return mapToResponse(appointment);
    }

    public List<AppointmentResponse> getAppointmentsByCustomerEmail(String customerEmail) {
        List<Appointment> appointments = appointmentRepository
                .findByCustomerEmailOrderByTimeSlotSlotDateAscTimeSlotStartTimeAsc(customerEmail);

        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getAppointmentsByUsername(String username) {
        log.debug("Fetching appointments for username: {}", username);
        List<Appointment> appointments = appointmentRepository
                .findByUsernameOrderBySlotDateAndStartTime(username);

        log.info("Found {} appointments for username: {}", appointments.size(), username);
        return appointments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelAppointment(String bookingReference) {
        log.info("Starting cancellation for appointment: {}", bookingReference);
        Appointment appointment = appointmentRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        // Check if appointment is already cancelled
        if ("CANCELLED".equals(appointment.getStatus())) {
            throw new IllegalStateException("Appointment is already cancelled");
        }

        // Store the time slot before updating appointment
        TimeSlot timeSlot = appointment.getTimeSlot();

        // Update appointment status
        appointment.setStatus("CANCELLED");
        appointmentRepository.save(appointment);

        // Get current confirmed appointments count for this time slot (excluding cancelled ones)
        int currentBookedCount = appointmentRepository.countConfirmedAppointmentsByTimeSlot(timeSlot.getId());

        // Update the time slot booked count
        timeSlot.setBookedCount(currentBookedCount);

        // If there's now capacity, mark as available
        if (currentBookedCount < timeSlot.getCapacity()) {
            timeSlot.setAvailable(true);
            log.info("Time slot {} now has capacity. Marking as available.", timeSlot.getId());
        } else {
            timeSlot.setAvailable(false);
            log.debug("Time slot {} remains unavailable due to full capacity.", timeSlot.getId());
        }

        timeSlotRepository.save(timeSlot);

        // Send cancellation email
        emailService.sendAppointmentCancellation(appointment);

        log.info("Appointment cancelled successfully. Reference: {}, Customer: {}",
                bookingReference, appointment.getCustomerEmail());
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        log.trace("Mapping appointment to response for ID: {}", appointment.getId());
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setCustomerName(appointment.getCustomerName());
        response.setCustomerEmail(appointment.getCustomerEmail());
        response.setCustomerPhone(appointment.getCustomerPhone());
        response.setBookingReference(appointment.getBookingReference());
        response.setStatus(appointment.getStatus());
        response.setAppointmentDate(appointment.getTimeSlot().getSlotDate());
        response.setStartTime(appointment.getTimeSlot().getStartTime());
        response.setEndTime(appointment.getTimeSlot().getEndTime());
        response.setBranchName(appointment.getTimeSlot().getBranch().getName());
        response.setBranchAddress(appointment.getTimeSlot().getBranch().getAddress());
        log.trace("Appointment mapping completed for ID: {}", appointment.getId());
        return response;
    }

}

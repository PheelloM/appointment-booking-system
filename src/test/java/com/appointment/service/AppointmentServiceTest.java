package com.appointment.service;

import com.appointment.dto.AppointmentRequest;
import com.appointment.dto.AppointmentResponse;
import com.appointment.entity.Appointment;
import com.appointment.entity.Branch;
import com.appointment.entity.TimeSlot;
import com.appointment.exception.DoubleBookingException;
import com.appointment.exception.ResourceNotFoundException;
import com.appointment.exception.SlotNotAvailableException;
import com.appointment.repository.AppointmentRepository;
import com.appointment.repository.TimeSlotRepository;
import com.appointment.util.BookingReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BookingReferenceGenerator bookingReferenceGenerator;

    @InjectMocks
    private AppointmentService appointmentService;

    @Captor
    private ArgumentCaptor<Appointment> appointmentCaptor;

    @Captor
    private ArgumentCaptor<TimeSlot> timeSlotCaptor;

    private AppointmentRequest validRequest;
    private TimeSlot availableTimeSlot;
    private Branch testBranch;
    private Appointment savedAppointment;

    @BeforeEach
    void setUp() {
        // Create the service with mocked dependencies
        appointmentService = new AppointmentService(
                appointmentRepository,
                timeSlotRepository,
                emailService,
                bookingReferenceGenerator
        );
        validRequest = createAppointmentRequest();
        testBranch = createTestBranch();
        availableTimeSlot = createAvailableTimeSlot();
        savedAppointment = createTestAppointment();
    }

    @Test
    void createAppointment_WithValidRequest_ShouldSuccess() {
        // Given
        String expectedReference = "APT-20240115-DEF456";

        // Only stub methods that are actually called in the success path
        when(timeSlotRepository.findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        )).thenReturn(Optional.of(availableTimeSlot));

        when(timeSlotRepository.existsByTimeSlotAndCustomerEmail(
                availableTimeSlot.getId(),
                validRequest.getCustomerEmail()
        )).thenReturn(false);

        when(appointmentRepository.countConfirmedAppointmentsByTimeSlot(
                availableTimeSlot.getId()
        )).thenReturn(0);
        when(bookingReferenceGenerator.generateBookingReference())
                .thenReturn(expectedReference);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment app = invocation.getArgument(0);
                    app.setId(1L);
                    app.setBookingReference(expectedReference);
                    return app;
                });

        // When
        AppointmentResponse response = appointmentService.createAppointment(validRequest);

        // Then
        assertNotNull(response);
        assertEquals("Peter Test", response.getCustomerName());
        assertEquals("peter@example.com", response.getCustomerEmail());
        assertNotNull(response.getBookingReference());
        assertEquals(expectedReference, response.getBookingReference());
        assertEquals("CONFIRMED", response.getStatus());

        //Verify
        verify(bookingReferenceGenerator).generateBookingReference();
        verify(timeSlotRepository).findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        );
        verify(timeSlotRepository).existsByTimeSlotAndCustomerEmail(
                availableTimeSlot.getId(),
                validRequest.getCustomerEmail()
        );
        verify(appointmentRepository).save(any(Appointment.class));
        verify(emailService).sendAppointmentConfirmation(any(Appointment.class));
        // Verify time slot was updated with correct booked count
        verify(timeSlotRepository).save(timeSlotCaptor.capture());
        TimeSlot updatedTimeSlot = timeSlotCaptor.getValue();
        assertEquals(1, updatedTimeSlot.getBookedCount());
        assertTrue(updatedTimeSlot.getAvailable());
    }

    @Test
    void createAppointment_TimeSlotNotAvailable_ShouldThrowException() {
        // Given
        when(timeSlotRepository.findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        )).thenReturn(Optional.empty());

        // When & Then
        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> appointmentService.createAppointment(validRequest)
        );

        assertEquals("Time slot not available", exception.getMessage());

        verify(timeSlotRepository, never()).existsByTimeSlotAndCustomerEmail(any(), any());
        verify(appointmentRepository, never()).save(any());
        verify(emailService, never()).sendAppointmentConfirmation(any());
    }

    @Test
    void createAppointment_CustomerAlreadyBookedSameSlot_ShouldThrowDoubleBookingException() {
        // Given
        when(timeSlotRepository.findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        )).thenReturn(Optional.of(availableTimeSlot));

        when(timeSlotRepository.existsByTimeSlotAndCustomerEmail(
                availableTimeSlot.getId(),
                validRequest.getCustomerEmail()
        )).thenReturn(true);

        // When & Then
        DoubleBookingException exception = assertThrows(
                DoubleBookingException.class,
                () -> appointmentService.createAppointment(validRequest)
        );

        assertEquals("Customer already has an appointment for this time slot", exception.getMessage());

        verify(appointmentRepository, never()).save(any());
        verify(emailService, never()).sendAppointmentConfirmation(any());
    }

    @Test
    void createAppointment_TimeSlotFullyBooked_ShouldThrowSlotNotAvailableException() {
        // Given
        availableTimeSlot.setCapacity(2);

        when(timeSlotRepository.findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        )).thenReturn(Optional.of(availableTimeSlot));

        when(timeSlotRepository.existsByTimeSlotAndCustomerEmail(
                availableTimeSlot.getId(),
                validRequest.getCustomerEmail()
        )).thenReturn(false);

        when(appointmentRepository.countConfirmedAppointmentsByTimeSlot(
                availableTimeSlot.getId()
        )).thenReturn(2); // Already 2 bookings

        // When & Then
        SlotNotAvailableException exception = assertThrows(
                SlotNotAvailableException.class,
                () -> appointmentService.createAppointment(validRequest)
        );

        assertEquals("Time slot is fully booked", exception.getMessage());

        // Verify time slot was marked as unavailable
        verify(timeSlotRepository).save(timeSlotCaptor.capture());
        TimeSlot updatedTimeSlot = timeSlotCaptor.getValue();
        assertFalse(updatedTimeSlot.getAvailable());

        verify(appointmentRepository, never()).save(any());
        verify(emailService, never()).sendAppointmentConfirmation(any());
    }

    @Test
    void createAppointment_TimeSlotBecomesFullyBooked_ShouldMarkAsUnavailable() {
        // Given - Time slot with capacity 1, currently has 0 bookings
        availableTimeSlot.setCapacity(1);
        availableTimeSlot.setBookedCount(0);
        availableTimeSlot.setAvailable(true);

        when(timeSlotRepository.findAvailableSlot(
                validRequest.getBranchId(),
                validRequest.getAppointmentDate(),
                validRequest.getStartTime()
        )).thenReturn(Optional.of(availableTimeSlot));

        when(timeSlotRepository.existsByTimeSlotAndCustomerEmail(
                availableTimeSlot.getId(),
                validRequest.getCustomerEmail()
        )).thenReturn(false);

        // Mock the count to return 0 (slot is not full yet)
        when(appointmentRepository.countConfirmedAppointmentsByTimeSlot(
                availableTimeSlot.getId()
        )).thenReturn(0);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment app = invocation.getArgument(0);
                    app.setId(1L);
                    app.setBookingReference("APT-ABC123");
                    return app;
                });

        // When
        AppointmentResponse response = appointmentService.createAppointment(validRequest);

        // Then
        assertNotNull(response);

        // Verify time slot was saved ONCE (after the booking, to update bookedCount and mark as unavailable)
        verify(timeSlotRepository, times(1)).save(timeSlotCaptor.capture());

        TimeSlot updatedTimeSlot = timeSlotCaptor.getValue();
        assertEquals(1, updatedTimeSlot.getBookedCount()); // Should be 1 (0 + 1)
        assertFalse(updatedTimeSlot.getAvailable()); // Should be unavailable since capacity reached
    }

    @Test
    void getAppointmentByReference_WithValidReference_ShouldReturnAppointment() {
        // Given
        String bookingReference = "APT-ABC123";
        when(appointmentRepository.findByBookingReference(bookingReference))
                .thenReturn(Optional.of(savedAppointment));

        // When
        AppointmentResponse response = appointmentService.getAppointmentByReference(bookingReference);

        // Then
        assertNotNull(response);
        assertEquals(bookingReference, response.getBookingReference());
        assertEquals("Peter Test", response.getCustomerName());
        assertEquals("Test Branch", response.getBranchName());

        verify(appointmentRepository).findByBookingReference(bookingReference);
    }

    @Test
    void getAppointmentByReference_WithInvalidReference_ShouldThrowException() {
        // Given
        String invalidReference = "INVALID-REF";
        when(appointmentRepository.findByBookingReference(invalidReference))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.getAppointmentByReference(invalidReference)
        );

        assertEquals("Appointment not found", exception.getMessage());
        verify(appointmentRepository).findByBookingReference(invalidReference);
    }

    @Test
    void cancelAppointment_WithValidReference_ShouldCancelAndUpdateTimeSlot() {
        // Given
        String bookingReference = "APT-ABC123";

        // Set up the appointment with a time slot that has some bookings
        savedAppointment.getTimeSlot().setBookedCount(2); // Currently has 2 bookings
        savedAppointment.getTimeSlot().setAvailable(false); // Currently unavailable (full)

        when(appointmentRepository.findByBookingReference(bookingReference))
                .thenReturn(Optional.of(savedAppointment));

        // Mock the count to return 1 after cancellation (2 bookings - 1 cancelled = 1 remaining)
        when(appointmentRepository.countConfirmedAppointmentsByTimeSlot(
                savedAppointment.getTimeSlot().getId()
        )).thenReturn(1);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        appointmentService.cancelAppointment(bookingReference);

        // Then
        verify(appointmentRepository).findByBookingReference(bookingReference);
        verify(appointmentRepository).save(appointmentCaptor.capture());

        Appointment cancelledAppointment = appointmentCaptor.getValue();
        assertEquals("CANCELLED", cancelledAppointment.getStatus());

        // Verify time slot was updated
        verify(timeSlotRepository).save(timeSlotCaptor.capture());
        TimeSlot updatedTimeSlot = timeSlotCaptor.getValue();

        // Should have 1 remaining booking (was 2, cancelled 1)
        assertEquals(1, updatedTimeSlot.getBookedCount());

        // Should be available now since bookedCount (1) < capacity (3)
        assertTrue(updatedTimeSlot.getAvailable());

        verify(emailService).sendAppointmentCancellation(any(Appointment.class));
    }

    @Test
    void cancelAppointment_WithInvalidReference_ShouldThrowException() {
        // Given
        String invalidReference = "INVALID-REF";
        when(appointmentRepository.findByBookingReference(invalidReference))
                .thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointment(invalidReference)
        );

        assertEquals("Appointment not found", exception.getMessage());
        verify(appointmentRepository, never()).save(any());
        verify(timeSlotRepository, never()).save(any());
        verify(emailService, never()).sendAppointmentCancellation(any());
    }

    @Test
    void createAppointment_ShouldGenerateUniqueBookingReference() {
        // Given
        String expectedReference = "APT-20240115-ABC123";
        when(timeSlotRepository.findAvailableSlot(any(), any(), any()))
                .thenReturn(Optional.of(availableTimeSlot));
        when(timeSlotRepository.existsByTimeSlotAndCustomerEmail(any(), any()))
                .thenReturn(false);
        when(appointmentRepository.countConfirmedAppointmentsByTimeSlot(any()))
                .thenReturn(0);
        // Mock the booking reference generator
        when(bookingReferenceGenerator.generateBookingReference())
                .thenReturn(expectedReference);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment app = invocation.getArgument(0);
                    app.setId(1L);
                    // The booking reference should already be set by the service
                    assertNotNull(app.getBookingReference());
                    assertEquals(expectedReference, app.getBookingReference());
                    return app;
                });
        // When
        AppointmentResponse response = appointmentService.createAppointment(validRequest);

        // Then
        assertNotNull(response);
        assertNotNull(response.getBookingReference());
        assertEquals(expectedReference, response.getBookingReference());
        // Verify the booking reference generator was called
        verify(bookingReferenceGenerator).generateBookingReference();
    }

    // Helper methods
    private AppointmentRequest createAppointmentRequest() {
        AppointmentRequest request = new AppointmentRequest();
        request.setCustomerName("Peter Test");
        request.setCustomerEmail("peter@example.com");
        request.setCustomerPhone("+1-555-0100");
        request.setBranchId(1L);
        request.setAppointmentDate(LocalDate.now().plusDays(1));
        request.setStartTime(LocalTime.of(9, 0));
        return request;
    }

    private Branch createTestBranch() {
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Test Branch");
        branch.setAddress("123 Test Street");
        branch.setPhone("123-456-7890");
        branch.setEmail("test@branch.com");
        branch.setOperatingHours("9AM-5PM");
        return branch;
    }

    private Appointment createTestAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setCustomerName("Peter Test");
        appointment.setCustomerEmail("peter@example.com");
        appointment.setCustomerPhone("+1-555-0100");
        appointment.setTimeSlot(availableTimeSlot);
        appointment.setBookingReference("APT-ABC123");
        appointment.setStatus("CONFIRMED");
        return appointment;
    }

    private TimeSlot createAvailableTimeSlot() {
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setBranch(testBranch);
        timeSlot.setSlotDate(LocalDate.now().plusDays(1));
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(9, 30));
        timeSlot.setCapacity(3);
        timeSlot.setBookedCount(0);
        timeSlot.setAvailable(true);
        return timeSlot;
    }
}
package com.appointment.service;

import com.appointment.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendAppointmentConfirmation(Appointment appointment) {
        // In production, integrate with actual email service (SendGrid, AWS SES, etc.)
        log.info("Sending confirmation email for appointment: {}", appointment.getBookingReference());
        log.info("To: {}", appointment.getCustomerEmail());
        log.info("Subject: Appointment Confirmation - Reference: {}", appointment.getBookingReference());
        log.info("Body: Dear {}, your appointment at {} on {} from {} to {} has been confirmed.",
                appointment.getCustomerName(),
                appointment.getTimeSlot().getBranch().getName(),
                appointment.getTimeSlot().getSlotDate(),
                appointment.getTimeSlot().getStartTime(),
                appointment.getTimeSlot().getEndTime());
    }

    public void sendAppointmentCancellation(Appointment appointment) {
        log.info("Sending cancellation email for appointment: {}", appointment.getBookingReference());
        log.info("To: {}", appointment.getCustomerEmail());
        log.info("Subject: Appointment Cancelled - Reference: {}", appointment.getBookingReference());
    }
}
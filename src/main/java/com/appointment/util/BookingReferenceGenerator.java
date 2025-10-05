package com.appointment.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class BookingReferenceGenerator {
    /**
     * Generates a unique booking reference
     * Format: APT-YYYYMMDD-XXXXXX
     * Where XXXXXX is a 6-character alphanumeric code
     */
    public String generateBookingReference() {
        String timestamp = LocalDateTime.now().toString().substring(0, 10).replace("-", "");
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String shortCode = uuid.substring(0, 6);
        return "APT-" + timestamp + "-" + shortCode;
    }
}

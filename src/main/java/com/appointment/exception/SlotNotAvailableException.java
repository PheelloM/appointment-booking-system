package com.appointment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SlotNotAvailableException extends RuntimeException {
    public SlotNotAvailableException(String message) {
        super(message);
    }

    public SlotNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

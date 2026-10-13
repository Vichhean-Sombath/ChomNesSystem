package com.chomnes.system.exception;

public class TimeSlotNotFoundException extends RuntimeException {
    public TimeSlotNotFoundException(Integer timeSlotId) {
        super("Time slot " + timeSlotId + " was not found.");
    }
}

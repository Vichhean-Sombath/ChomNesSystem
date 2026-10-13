package com.chomnes.system.exception;

public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException(Integer scheduleId) {
        super("Schedule " + scheduleId + " was not found.");
    }
}

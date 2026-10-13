package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateTimeSlotRequest;
import com.chomnes.system.dto.TimeSlotResponse;
import com.chomnes.system.dto.UpdateTimeSlotRequest;
import com.chomnes.system.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {
    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping
    public List<TimeSlotResponse> findAll() {
        return timeSlotService.findAll();
    }

    @GetMapping("/{timeSlotId}")
    public TimeSlotResponse findById(@PathVariable Integer timeSlotId) {
        return timeSlotService.findById(timeSlotId);
    }

    @PostMapping
    public ResponseEntity<TimeSlotResponse> create(@Valid @RequestBody CreateTimeSlotRequest request) {
        TimeSlotResponse created = timeSlotService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{timeSlotId}")
                .buildAndExpand(created.timeSlotId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{timeSlotId}")
    public TimeSlotResponse update(
            @PathVariable Integer timeSlotId,
            @Valid @RequestBody UpdateTimeSlotRequest request
    ) {
        return timeSlotService.update(timeSlotId, request);
    }
}

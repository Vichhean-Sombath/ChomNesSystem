package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateScheduleRequest;
import com.chomnes.system.dto.ScheduleResponse;
import com.chomnes.system.dto.UpdateScheduleRequest;
import com.chomnes.system.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/schedules", "/schedule"})
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping({"", "/data"})
    public List<ScheduleResponse> findAll() {
        return scheduleService.findAll();
    }

    @GetMapping({"/{scheduleId}", "/data/{scheduleId}"})
    public ScheduleResponse findById(@PathVariable Integer scheduleId) {
        return scheduleService.findById(scheduleId);
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody CreateScheduleRequest request) {
        ScheduleResponse created = scheduleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{scheduleId}")
                .buildAndExpand(created.scheduleId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{scheduleId}", "/update/{scheduleId}"})
    public ScheduleResponse update(
            @PathVariable Integer scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        return scheduleService.update(scheduleId, request);
    }

    @DeleteMapping({"/{scheduleId}", "/delete/{scheduleId}"})
    public Map<String, String> delete(@PathVariable Integer scheduleId) {
        scheduleService.delete(scheduleId);
        return Map.of("message", "deleted");
    }
}

package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateSemesterRequest;
import com.chomnes.system.dto.SemesterResponse;
import com.chomnes.system.dto.UpdateSemesterRequest;
import com.chomnes.system.service.SemesterService;
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
@RequestMapping({"/api/semesters", "/semester"})
public class SemesterController {
    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping({"", "/data"})
    public List<SemesterResponse> findAll() {
        return semesterService.findAll();
    }

    @GetMapping({"/{semesterId}", "/data/{semesterId}"})
    public SemesterResponse findById(@PathVariable Integer semesterId) {
        return semesterService.findById(semesterId);
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<SemesterResponse> create(@Valid @RequestBody CreateSemesterRequest request) {
        SemesterResponse created = semesterService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{semesterId}")
                .buildAndExpand(created.semesterId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{semesterId}", "/update/{semesterId}"})
    public SemesterResponse update(
            @PathVariable Integer semesterId,
            @Valid @RequestBody UpdateSemesterRequest request
    ) {
        return semesterService.update(semesterId, request);
    }

    @DeleteMapping({"/{semesterId}", "/delete/{semesterId}"})
    public Map<String, String> delete(@PathVariable Integer semesterId) {
        semesterService.delete(semesterId);
        return Map.of("message", "deleted");
    }
}

package com.chomnes.system.controller;

import com.chomnes.system.dto.AcademicYearResponse;
import com.chomnes.system.dto.CreateAcademicYearRequest;
import com.chomnes.system.dto.UpdateAcademicYearRequest;
import com.chomnes.system.service.AcademicYearService;
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
@RequestMapping({"/api/academic-years", "/academic-year"})
public class AcademicYearController {
    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @GetMapping({"", "/data"})
    public List<AcademicYearResponse> findAll() {
        return academicYearService.findAll();
    }

    @GetMapping({"/{academicYearId}", "/data/{academicYearId}"})
    public AcademicYearResponse findById(@PathVariable Integer academicYearId) {
        return academicYearService.findById(academicYearId);
    }

    @PostMapping({"", "/create"})
    public ResponseEntity<AcademicYearResponse> create(@Valid @RequestBody CreateAcademicYearRequest request) {
        AcademicYearResponse created = academicYearService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{academicYearId}")
                .buildAndExpand(created.academicYearId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping({"/{academicYearId}", "/update/{academicYearId}"})
    public AcademicYearResponse update(
            @PathVariable Integer academicYearId,
            @Valid @RequestBody UpdateAcademicYearRequest request
    ) {
        return academicYearService.update(academicYearId, request);
    }

    @DeleteMapping({"/{academicYearId}", "/delete/{academicYearId}"})
    public Map<String, String> delete(@PathVariable Integer academicYearId) {
        academicYearService.delete(academicYearId);
        return Map.of("message", "deleted");
    }
}

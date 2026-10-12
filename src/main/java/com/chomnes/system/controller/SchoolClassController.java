package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateSchoolClassRequest;
import com.chomnes.system.dto.SchoolClassResponse;
import com.chomnes.system.dto.UpdateSchoolClassRequest;
import com.chomnes.system.service.SchoolClassService;
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
@RequestMapping("/api/classes")
public class SchoolClassController {
    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @GetMapping
    public List<SchoolClassResponse> findAll() {
        return schoolClassService.findAll();
    }

    @GetMapping("/{classId}")
    public SchoolClassResponse findById(@PathVariable Integer classId) {
        return schoolClassService.findById(classId);
    }

    @PostMapping
    public ResponseEntity<SchoolClassResponse> create(@Valid @RequestBody CreateSchoolClassRequest request) {
        SchoolClassResponse created = schoolClassService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{classId}")
                .buildAndExpand(created.classId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{classId}")
    public SchoolClassResponse update(
            @PathVariable Integer classId,
            @Valid @RequestBody UpdateSchoolClassRequest request
    ) {
        return schoolClassService.update(classId, request);
    }
}

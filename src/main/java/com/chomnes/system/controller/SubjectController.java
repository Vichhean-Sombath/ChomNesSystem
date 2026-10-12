package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateSubjectRequest;
import com.chomnes.system.dto.SubjectResponse;
import com.chomnes.system.dto.UpdateSubjectRequest;
import com.chomnes.system.service.SubjectService;
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
@RequestMapping("/api/subjects")
public class SubjectController {
    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public List<SubjectResponse> findAll() {
        return subjectService.findAll();
    }

    @GetMapping("/{subjectId}")
    public SubjectResponse findById(@PathVariable Integer subjectId) {
        return subjectService.findById(subjectId);
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse created = subjectService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{subjectId}")
                .buildAndExpand(created.subjectId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{subjectId}")
    public SubjectResponse update(
            @PathVariable Integer subjectId,
            @Valid @RequestBody UpdateSubjectRequest request
    ) {
        return subjectService.update(subjectId, request);
    }
}

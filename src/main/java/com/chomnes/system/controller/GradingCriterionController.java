package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateGradingCriterionRequest;
import com.chomnes.system.dto.GradingCriterionResponse;
import com.chomnes.system.dto.UpdateGradingCriterionRequest;
import com.chomnes.system.service.GradingCriterionService;
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
@RequestMapping("/api/grading-criteria")
public class GradingCriterionController {
    private final GradingCriterionService criterionService;

    public GradingCriterionController(GradingCriterionService criterionService) {
        this.criterionService = criterionService;
    }

    @GetMapping
    public List<GradingCriterionResponse> findAll() {
        return criterionService.findAll();
    }

    @GetMapping("/{criteriaId}")
    public GradingCriterionResponse findById(@PathVariable Integer criteriaId) {
        return criterionService.findById(criteriaId);
    }

    @PostMapping
    public ResponseEntity<GradingCriterionResponse> create(
            @Valid @RequestBody CreateGradingCriterionRequest request
    ) {
        GradingCriterionResponse created = criterionService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{criteriaId}")
                .buildAndExpand(created.criteriaId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{criteriaId}")
    public GradingCriterionResponse update(
            @PathVariable Integer criteriaId,
            @Valid @RequestBody UpdateGradingCriterionRequest request
    ) {
        return criterionService.update(criteriaId, request);
    }
}

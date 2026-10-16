package com.chomnes.system.controller;

import com.chomnes.system.dto.CreateGradingCriterionRequest;
import com.chomnes.system.dto.GradingCriterionResponse;
import com.chomnes.system.dto.UpdateGradingCriterionRequest;
import com.chomnes.system.service.GradingCriterionService;
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
@RequestMapping({"/api/grading-criteria", "/grading-criteria"})
public class GradingCriterionController {
    private final GradingCriterionService criterionService;

    public GradingCriterionController(GradingCriterionService criterionService) {
        this.criterionService = criterionService;
    }

    @GetMapping({"", "/data"})
    public List<GradingCriterionResponse> findAll() {
        return criterionService.findAll();
    }

    @GetMapping({"/{criteriaId}", "/data/{criteriaId}"})
    public GradingCriterionResponse findById(@PathVariable Integer criteriaId) {
        return criterionService.findById(criteriaId);
    }

    @PostMapping({"", "/create"})
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

    @PutMapping({"/{criteriaId}", "/update/{criteriaId}"})
    public GradingCriterionResponse update(
            @PathVariable Integer criteriaId,
            @Valid @RequestBody UpdateGradingCriterionRequest request
    ) {
        return criterionService.update(criteriaId, request);
    }

    @DeleteMapping({"/{criteriaId}", "/delete/{criteriaId}"})
    public Map<String, String> delete(@PathVariable Integer criteriaId) {
        criterionService.delete(criteriaId);
        return Map.of("message", "deleted");
    }
}

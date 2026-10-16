package com.chomnes.system.controller;

import com.chomnes.system.service.ReportsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) Integer academicYearId,
            @RequestParam(required = false) Integer semesterId
    ) {
        return reportsService.summary(range, academicYearId, semesterId);
    }
}

package com.evcharging.controller;

import com.evcharging.dto.ReportSummary;
import com.evcharging.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** ADMIN -> system-wide summary. Everyone else -> their own personal summary. */
    @GetMapping("/summary")
    public ReportSummary summary() {
        return reportService.summary();
    }
}

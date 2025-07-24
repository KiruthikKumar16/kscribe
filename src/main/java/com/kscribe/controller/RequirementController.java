package com.kscribe.controller;

import com.kscribe.model.Requirement;
import com.kscribe.model.TestCase;
import com.kscribe.model.TestCaseResponse;
import com.kscribe.service.TestCaseGeneratorService;
import com.kscribe.util.CsvExportUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
public class RequirementController {
    private final TestCaseGeneratorService testCaseGeneratorService;
    private final CsvExportUtil csvExportUtil;

    @PostMapping("/to-testcases")
    public TestCaseResponse generateTestCases(@RequestBody List<Requirement> requirements) {
        List<TestCase> testCases = testCaseGeneratorService.generateTestCases(requirements);
        return new TestCaseResponse(testCases);
    }

    @PostMapping("/to-csv")
    public ResponseEntity<byte[]> generateTestCasesCsv(@RequestBody List<Requirement> requirements) {
        List<TestCase> testCases = testCaseGeneratorService.generateTestCases(requirements);
        byte[] csv = csvExportUtil.exportTestCasesToCsv(testCases);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=kscribe_testcases.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
} 
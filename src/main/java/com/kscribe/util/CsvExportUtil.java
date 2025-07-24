package com.kscribe.util;

import com.kscribe.model.TestCase;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.List;

@Component
public class CsvExportUtil {
    public byte[] exportTestCasesToCsv(List<TestCase> testCases) {
        StringWriter writer = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(writer);
        String[] header = {"ID", "Title", "Description", "Preconditions", "Steps", "Expected Output", "Severity", "Tags"};
        csvWriter.writeNext(header);
        for (TestCase tc : testCases) {
            csvWriter.writeNext(new String[] {
                    tc.getId(),
                    tc.getTitle(),
                    tc.getDescription(),
                    String.join("; ", tc.getPreconditions()),
                    String.join("; ", tc.getSteps()),
                    tc.getExpectedOutput(),
                    tc.getSeverity(),
                    tc.getTags() == null ? "" : String.join(", ", tc.getTags())
            });
        }
        try {
            csvWriter.close();
        } catch (Exception ignored) {}
        return writer.toString().getBytes();
    }
} 
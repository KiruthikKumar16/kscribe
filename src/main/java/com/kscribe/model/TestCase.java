package com.kscribe.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {
    private String id;
    private String title;
    private String description;
    private List<String> preconditions;
    private List<String> steps;
    private String expectedOutput;
    private String severity;
    private List<String> tags;
} 
package com.kscribe.service;

import com.kscribe.model.Requirement;
import com.kscribe.model.TestCase;
import com.kscribe.nlp.NlpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TestCaseGeneratorService {
    private final NlpService nlpService;

    public List<TestCase> generateTestCases(List<Requirement> requirements) {
        List<TestCase> testCases = new ArrayList<>();
        int counter = 1;
        for (Requirement req : requirements) {
            List<String> sentences = nlpService.sentences(req.getText());
            for (String sentence : sentences) {
                // Check for conditional logic: if/then/else/else if
                if (sentence.toLowerCase().contains("if ") && (sentence.toLowerCase().contains("then") || sentence.toLowerCase().contains("else"))) {
                    // Parse logic tree
                    List<ConditionalBranch> branches = parseConditionTree(sentence);
                    for (ConditionalBranch branch : branches) {
                        TestCase tc = new TestCase();
                        tc.setId("TC" + counter++);
                        tc.setTitle(branch.title);
                        tc.setDescription(branch.condition);
                        tc.setPreconditions(Collections.singletonList(branch.condition));
                        tc.setSteps(Collections.singletonList("Step: " + branch.action));
                        tc.setExpectedOutput(branch.expectedOutput);
                        tc.setSeverity(branch.severity);
                        tc.setTags(branch.tags);
                        testCases.add(tc);
                    }
                } else {
                    // Fallback: use clause splitting as before
                    String[] clauses = sentence.split(" and | and|and | or | or|or |;|\\.|,");
                    for (String clause : clauses) {
                        clause = clause.trim();
                        if (clause.isEmpty()) continue;
                        boolean isNegation = detectNegation(clause);
                        boolean isConditional = detectConditional(clause);
                        TestCase tc = new TestCase();
                        tc.setId("TC" + counter++);
                        tc.setTitle(generateTitle(clause, isNegation, isConditional));
                        tc.setDescription(clause);
                        tc.setPreconditions(generatePreconditions(clause));
                        tc.setSteps(generateSteps(clause, isConditional));
                        tc.setExpectedOutput(generateExpected(clause, isNegation));
                        tc.setSeverity(detectSeverity(clause));
                        tc.setTags(generateTags(clause, isNegation, isConditional));
                        testCases.add(tc);
                        if (!isNegation) {
                            String negated = negateSentence(clause);
                            if (!negated.equals(clause)) {
                                TestCase negTc = new TestCase();
                                negTc.setId("TC" + counter++);
                                negTc.setTitle("Negative: " + generateTitle(negated, true, isConditional));
                                negTc.setDescription(negated);
                                negTc.setPreconditions(generatePreconditions(negated));
                                negTc.setSteps(generateSteps(negated, isConditional));
                                negTc.setExpectedOutput("Error message displayed");
                                negTc.setSeverity("High");
                                negTc.setTags(generateTags(negated, true, isConditional));
                                testCases.add(negTc);
                            }
                        }
                    }
                }
            }
        }
        return testCases;
    }

    // Helper class for conditional branches
    private static class ConditionalBranch {
        String title;
        String condition;
        String action;
        String expectedOutput;
        String severity;
        List<String> tags;
    }

    // Parse a logic tree from a conditional sentence
    private List<ConditionalBranch> parseConditionTree(String sentence) {
        List<ConditionalBranch> branches = new ArrayList<>();
        String lower = sentence.toLowerCase();
        // Split on 'else if', 'else', and 'if'
        String[] parts = lower.split("else if|else|if ");
        List<String> keywords = new ArrayList<>();
        int idx = 0;
        while (idx < lower.length()) {
            if (lower.startsWith("if ", idx)) { keywords.add("if"); idx += 3; }
            else if (lower.startsWith("else if", idx)) { keywords.add("else if"); idx += 7; }
            else if (lower.startsWith("else", idx)) { keywords.add("else"); idx += 4; }
            else { idx++; }
        }
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) continue;
            ConditionalBranch branch = new ConditionalBranch();
            String keyword = (i < keywords.size()) ? keywords.get(i) : "";
            if (keyword.equals("if") || keyword.equals("else if")) {
                // Try to split on 'then'
                String[] condAction = part.split("then", 2);
                branch.condition = condAction[0].trim();
                branch.action = condAction.length > 1 && !condAction[1].trim().isEmpty() ? condAction[1].trim() : condAction[0].trim();
            } else if (keyword.equals("else")) {
                branch.condition = "otherwise";
                branch.action = part;
                // Use action as description and precondition for 'otherwise'
                branch.condition = branch.action;
            } else {
                branch.condition = part;
                branch.action = part;
            }
            // Fallbacks for empty action/condition
            if (branch.action == null || branch.action.isEmpty()) branch.action = branch.condition;
            if (branch.condition == null || branch.condition.isEmpty()) branch.condition = branch.action;
            // Heuristics for title, output, severity, tags
            branch.title = generateTitle(branch.action, detectNegation(branch.action), detectConditional(branch.action));
            branch.expectedOutput = generateExpected(branch.action, detectNegation(branch.action));
            branch.severity = detectSeverity(branch.action);
            branch.tags = generateTags(branch.action, detectNegation(branch.action), detectConditional(branch.action));
            branches.add(branch);
        }
        return branches;
    }

    private boolean detectNegation(String sentence) {
        String s = sentence.toLowerCase();
        return s.contains("not ") || s.contains("should not") || s.contains("must not") || s.contains("cannot") || s.contains("can't") || s.contains("won't") || s.contains("never");
    }

    private boolean detectConditional(String sentence) {
        return Pattern.compile("\\bif ", Pattern.CASE_INSENSITIVE).matcher(sentence).find();
    }

    private String negateSentence(String sentence) {
        // Simple negation for demo: add 'not' after first modal verb
        String[] modals = {"should", "must", "can", "will", "shall"};
        for (String modal : modals) {
            int idx = sentence.toLowerCase().indexOf(modal);
            if (idx != -1) {
                return sentence.substring(0, idx + modal.length()) + " not" + sentence.substring(idx + modal.length());
            }
        }
        return sentence;
    }

    private List<String> generatePreconditions(String sentence) {
        List<String> preconditions = new ArrayList<>();
        String s = sentence.toLowerCase();
        // Extract 'if' clauses as preconditions
        if (s.contains("if ")) {
            int idx = s.indexOf("if ");
            int thenIdx = s.indexOf("then", idx);
            if (thenIdx != -1) {
                preconditions.add(sentence.substring(idx, thenIdx).trim());
            } else {
                preconditions.add(sentence.substring(idx).trim());
            }
        }
        // Heuristic: look for explicit requirements (must, should, required)
        if (s.contains("must ") || s.contains("should ") || s.contains("required")) {
            preconditions.add("System requirement: " + sentence);
        }
        // Use POS tags to find modal verbs as preconditions
        List<String> posTags = nlpService.posTag(sentence);
        for (String tagged : posTags) {
            if (tagged.contains("MD")) { // Modal verb
                preconditions.add("Action requires: " + tagged.split("/")[0]);
            }
        }
        return preconditions;
    }

    private List<String> generateSteps(String sentence, boolean isConditional) {
        List<String> steps = new ArrayList<>();
        // Use punctuation and conjunctions to split steps
        String[] splitByAnd = sentence.split(" and |,|;|\\. ");
        for (String part : splitByAnd) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // Use lemmatization to normalize the action
                List<String> lemmas = nlpService.lemmatize(trimmed);
                String lemmaStep = String.join(" ", lemmas);
                steps.add("Step: " + lemmaStep);
            }
        }
        // For conditionals, also split on 'then', 'else'
        if (isConditional) {
            String[] parts = sentence.split("then|else", -1);
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !steps.contains("Step: " + trimmed)) {
                    steps.add("Step: " + trimmed);
                }
            }
        }
        return steps;
    }

    private String generateTitle(String sentence, boolean isNegation, boolean isConditional) {
        String s = sentence == null ? "" : sentence.toLowerCase();
        if (s.isEmpty() || s.equals("step:") || s.equals("otherwise")) return "Default scenario";
        if (isNegation) return "Negative scenario: " + summarize(s);
        if (isConditional) return "Conditional scenario: " + summarize(s);
        if (s.contains("login")) return "Validate login";
        if (s.contains("register")) return "User registration";
        if (s.contains("reset")) return "Password reset";
        if (s.contains("error")) return "Error handling";
        if (s.contains("warning")) return "Warning scenario";
        if (s.contains("access")) return "Access control";
        if (s.length() > 30) return summarize(s);
        return capitalizeFirst(s);
    }

    private String summarize(String s) {
        s = s.replaceAll("step:", "").replaceAll("[;.]$", "").trim();
        return capitalizeFirst(s);
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return "Test Case";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String generateExpected(String sentence, boolean isNegation) {
        if (isNegation) return "Error message displayed";
        if (sentence.toLowerCase().contains("error")) return "Error message displayed";
        if (sentence.toLowerCase().contains("success")) return "Operation successful";
        return "Expected result for: " + sentence;
    }

    private String detectSeverity(String sentence) {
        String s = sentence.toLowerCase();
        if (s.contains("security") || s.contains("critical") || s.contains("must not")) return "High";
        if (s.contains("should not") || s.contains("error")) return "Medium";
        return "Low";
    }

    private List<String> generateTags(String sentence, boolean isNegation, boolean isConditional) {
        List<String> tags = new ArrayList<>();
        String s = sentence.toLowerCase();
        if (isNegation) tags.add("negative");
        if (isConditional) tags.add("conditional");
        if (s.contains("security")) tags.add("security");
        if (s.contains("edge") || s.contains("empty") || s.contains("invalid") || s.contains("expired")) tags.add("edge-case");
        // Add tags for detected entities (user, email, password, etc.)
        if (s.contains("user")) tags.add("user");
        if (s.contains("email")) tags.add("email");
        if (s.contains("password")) tags.add("password");
        if (s.contains("registration")) tags.add("registration");
        if (s.contains("login")) tags.add("login");
        if (s.contains("reset")) tags.add("reset");
        return tags;
    }
} 
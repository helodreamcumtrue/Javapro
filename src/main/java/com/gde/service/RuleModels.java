package com.gde.service;

import java.util.List;

/**
 * Plain POJOs that mirror the structure of resources/rules.json.
 * Jackson deserializes the JSON dataset directly into these classes.
 * Kept in one file for simplicity since they are only used internally
 * by DecompositionService.
 */
public class RuleModels {

    /** Root of rules.json */
    public static class RuleSet {
        public List<TopicRule> topicRules;
        public List<VerbTemplate> verbTemplates;
        public List<RuleTask> defaultTemplate;
    }

    /** A rule matched against specific topic keywords, e.g. "web development" */
    public static class TopicRule {
        public List<String> keywords;
        public List<RuleTask> tasks;
    }

    /** A rule matched against an action verb, e.g. "learn", "build", "prepare" */
    public static class VerbTemplate {
        public List<String> keywords;
        /** description may contain the placeholder {topic} */
        public List<RuleTask> tasks;
    }

    /** A single task definition coming from the JSON dataset */
    public static class RuleTask {
        public String description;
        public String priority;       // HIGH / MEDIUM / LOW
        public int estimatedDays;
        public List<RuleTask> subtasks; // optional nested children (tree support)
    }
}

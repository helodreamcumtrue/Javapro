package com.gde.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gde.model.Task;
import com.gde.service.RuleModels.*;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ============================================================
 *  RULE-BASED GOAL DECOMPOSITION ENGINE
 * ============================================================
 * This is the "brain" of the project. It is 100% deterministic,
 * rule-based logic — NO AI/ML models, NO external API calls.
 *
 * Decomposition strategy (in priority order):
 *   1. TOPIC MATCH: if the goal text contains a known topic keyword
 *      (e.g. "web development", "python", "guitar") -> use the
 *      hand-crafted task list for that exact topic.
 *   2. VERB MATCH: else, if the goal contains a known action verb
 *      (e.g. "learn", "build", "prepare", "improve", "start") ->
 *      use a generic template for that verb type, substituting
 *      the remaining words of the goal as "{topic}".
 *   3. DEFAULT: else -> fall back to a generic 6-step generic plan.
 *
 * All rules live in resources/rules.json so the dataset can be
 * extended without touching Java code.
 * ============================================================
 */
@Service
public class DecompositionService {

    private RuleSet ruleSet;

    @PostConstruct
    public void loadRules() {
        try (InputStream is = new ClassPathResource("rules.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            this.ruleSet = mapper.readValue(is, RuleSet.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load rules.json dataset", e);
        }
    }

    /**
     * Main entry point: takes the raw goal title + difficulty and returns
     * a flat list of Task entities (goalId not yet set — caller assigns it),
     * already wired with parentId (tree), orderIndex, dependsOnTaskId
     * (sequential dependency chain) and a computed importance score.
     *
     * NOTE: ids are not set here (that happens on DB save), so parent-child
     * linking during the initial build uses a temporary negative-int key
     * system resolved by the caller (GoalController) after persistence.
     * To keep this simple and robust, we instead return a TaskBuildResult
     * tree structure that the controller persists top-down (parents first),
     * so real DB-generated ids can be used for parentId / dependsOnTaskId.
     */
    public static class DecompositionPlan {
        public List<TaskBuildResult> tasks;
        public String matchType;
        public String matchedKeyword;
        public String explanation;
        public String depth;
        public int totalTasks;
        public int totalPhases;
        public int totalSubtasks;

        public DecompositionPlan(List<TaskBuildResult> tasks, String matchType, String matchedKeyword, String explanation) {
            this(tasks, matchType, matchedKeyword, explanation, "in-depth");
        }

        public DecompositionPlan(List<TaskBuildResult> tasks, String matchType, String matchedKeyword, String explanation, String depth) {
            this.tasks = tasks;
            this.matchType = matchType;
            this.matchedKeyword = matchedKeyword;
            this.explanation = explanation;
            this.depth = (depth == null || depth.isBlank()) ? "in-depth" : depth;

            int phases = 0;
            int subtasks = 0;
            if (tasks != null) {
                for (TaskBuildResult t : tasks) {
                    phases++;
                    subtasks += countSubtasks(t);
                }
            }
            this.totalPhases = phases;
            this.totalSubtasks = subtasks;
            this.totalTasks = phases + subtasks;
        }

        private int countSubtasks(TaskBuildResult node) {
            if (node.children == null || node.children.isEmpty()) return 0;
            int count = node.children.size();
            for (TaskBuildResult child : node.children) {
                count += countSubtasks(child);
            }
            return count;
        }
    }

    public List<TaskBuildResult> decompose(String goalTitle, String difficulty) {
        return decompose(goalTitle, difficulty, "in-depth");
    }

    public List<TaskBuildResult> decompose(String goalTitle, String difficulty, String depth) {
        return decomposePlan(goalTitle, difficulty, depth).tasks;
    }

    public DecompositionPlan decomposePlan(String goalTitle, String difficulty) {
        return decomposePlan(goalTitle, difficulty, "in-depth");
    }

    public DecompositionPlan decomposePlan(String goalTitle, String difficulty, String depth) {
        String cleaned = cleanGoal(goalTitle);
        double multiplier = difficultyMultiplier(difficulty);
        boolean isInDepth = !"standard".equalsIgnoreCase(depth);
        String resolvedDepth = isInDepth ? "in-depth" : "standard";

        String matchType = "DEFAULT";
        String matchedKeyword = "general-plan";
        String explanation = isInDepth
                ? "Applied comprehensive 6-phase in-depth execution framework with granular subtasks"
                : "Applied standard 6-phase goal execution framework";

        // 1. Topic Match
        MatchedRule topicMatch = findTopicMatch(cleaned);
        List<RuleTask> matchedTasks = null;

        if (topicMatch != null) {
            matchedTasks = topicMatch.tasks;
            matchType = "TOPIC";
            matchedKeyword = topicMatch.keyword;
            explanation = isInDepth
                    ? "Matched in-depth specialized curriculum for '" + topicMatch.keyword + "' with granular execution sub-steps"
                    : "Matched specialized topic curriculum for '" + topicMatch.keyword + "'";
        }

        // 2. Verb Template Match
        if (matchedTasks == null) {
            MatchedVerb verbMatch = findVerbMatch(cleaned, goalTitle);
            if (verbMatch != null) {
                matchedTasks = verbMatch.tasks;
                matchType = "VERB";
                matchedKeyword = verbMatch.keyword;
                explanation = isInDepth
                        ? "Matched action verb template '" + verbMatch.keyword + "' targeting '" + verbMatch.topic + "' with full phase sub-steps"
                        : "Matched action verb template '" + verbMatch.keyword + "' targeting '" + verbMatch.topic + "'";
            }
        }

        // 3. Fallback Default Template
        if (matchedTasks == null) {
            matchedTasks = applyDefault(goalTitle);
            matchType = "DEFAULT";
            matchedKeyword = "default";
            explanation = isInDepth
                    ? "Applied comprehensive 6-phase goal execution framework with multi-tier milestones"
                    : "Applied standard 6-phase goal execution framework";
        }

        List<TaskBuildResult> result = buildTree(matchedTasks, multiplier, null, isInDepth);

        // HARD difficulty: add one extra advanced/deep-dive step at the end
        if ("hard".equalsIgnoreCase(difficulty)) {
            TaskBuildResult extra = new TaskBuildResult();
            extra.description = "Deep-dive: explore advanced topics and edge cases";
            extra.priority = "MEDIUM";
            extra.estimatedDays = Math.max(2, (int) Math.round(5 * multiplier));
            extra.children = new ArrayList<>();
            if (isInDepth) {
                TaskBuildResult sub1 = new TaskBuildResult();
                sub1.description = "Profile performance bottlenecks, memory allocation & stress limits";
                sub1.priority = "HIGH";
                sub1.estimatedDays = Math.max(1, (int) Math.round(2 * multiplier));
                extra.children.add(sub1);

                TaskBuildResult sub2 = new TaskBuildResult();
                sub2.description = "Implement security hardening, audit checklists & disaster recovery protocols";
                sub2.priority = "MEDIUM";
                sub2.estimatedDays = Math.max(1, (int) Math.round(2 * multiplier));
                extra.children.add(sub2);
            }
            result.add(extra);
        }

        assignOrderScoreAndDependencies(result, 0);
        return new DecompositionPlan(result, matchType, matchedKeyword, explanation, resolvedDepth);
    }

    private String cleanGoal(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static class MatchedRule {
        String keyword;
        List<RuleTask> tasks;
        MatchedRule(String keyword, List<RuleTask> tasks) {
            this.keyword = keyword;
            this.tasks = tasks;
        }
    }

    private static class MatchedVerb {
        String keyword;
        String topic;
        List<RuleTask> tasks;
        MatchedVerb(String keyword, String topic, List<RuleTask> tasks) {
            this.keyword = keyword;
            this.topic = topic;
            this.tasks = tasks;
        }
    }

    // ---------------------------------------------------------------
    // Matching logic
    // ---------------------------------------------------------------

    private MatchedRule findTopicMatch(String cleanedGoal) {
        for (TopicRule rule : ruleSet.topicRules) {
            for (String keyword : rule.keywords) {
                String cleanKeyword = cleanGoal(keyword);
                if (cleanedGoal.contains(cleanKeyword)) {
                    return new MatchedRule(keyword, rule.tasks);
                }
            }
        }
        return null;
    }

    private MatchedVerb findVerbMatch(String cleanedGoal, String originalGoal) {
        for (VerbTemplate template : ruleSet.verbTemplates) {
            for (String keyword : template.keywords) {
                String cleanKeyword = cleanGoal(keyword);
                if (cleanedGoal.contains(cleanKeyword)) {
                    String topic = extractTopic(cleanedGoal, cleanKeyword);
                    return new MatchedVerb(keyword, topic, substituteTopic(template.tasks, topic));
                }
            }
        }
        return null;
    }

    private List<RuleTask> applyDefault(String originalGoal) {
        return substituteTopic(ruleSet.defaultTemplate, originalGoal);
    }

    /** Removes the matched verb keyword from the goal string to get the remaining "topic" phrase. */
    private String extractTopic(String normalizedGoal, String keyword) {
        String topic = normalizedGoal.replaceFirst(keyword.toLowerCase(Locale.ROOT), "").trim();
        if (topic.isEmpty()) {
            topic = normalizedGoal;
        }
        return topic;
    }

    /** Replaces the {topic} / {goal} placeholder in every task description and nested subtask with the actual phrase. */
    private List<RuleTask> substituteTopic(List<RuleTask> templateTasks, String topic) {
        if (templateTasks == null) return new ArrayList<>();
        List<RuleTask> out = new ArrayList<>();
        for (RuleTask t : templateTasks) {
            RuleTask copy = new RuleTask();
            copy.description = (t.description != null)
                    ? t.description.replace("{topic}", topic).replace("{goal}", topic)
                    : "";
            copy.priority = t.priority;
            copy.estimatedDays = t.estimatedDays;
            if (t.subtasks != null && !t.subtasks.isEmpty()) {
                copy.subtasks = substituteTopic(t.subtasks, topic);
            } else {
                copy.subtasks = new ArrayList<>();
            }
            out.add(copy);
        }
        return out;
    }

    // ---------------------------------------------------------------
    // Tree building / dependency / scoring
    // ---------------------------------------------------------------

    private double difficultyMultiplier(String difficulty) {
        if (difficulty == null) return 1.0;
        switch (difficulty.toLowerCase(Locale.ROOT)) {
            case "easy":
                return 0.7;
            case "hard":
                return 1.5;
            default:
                return 1.0;
        }
    }

    private List<TaskBuildResult> buildTree(List<RuleTask> ruleTasks, double multiplier, TaskBuildResult parent, boolean isInDepth) {
        List<TaskBuildResult> nodes = new ArrayList<>();
        if (ruleTasks == null) return nodes;

        for (RuleTask rt : ruleTasks) {
            TaskBuildResult node = new TaskBuildResult();
            node.description = rt.description;
            node.priority = (rt.priority == null) ? "MEDIUM" : rt.priority.toUpperCase(Locale.ROOT);
            node.estimatedDays = Math.max(1, (int) Math.round(rt.estimatedDays * multiplier));

            if (isInDepth) {
                if (rt.subtasks != null && !rt.subtasks.isEmpty()) {
                    node.children = buildTree(rt.subtasks, multiplier, node, isInDepth);
                } else if (parent == null) {
                    // Top-level task lacking subtasks: synthesize contextual in-depth sub-steps!
                    List<RuleTask> synthesized = synthesizeInDepthSubtasks(rt.description, multiplier);
                    node.children = buildTree(synthesized, multiplier, node, isInDepth);
                } else {
                    node.children = new ArrayList<>();
                }
            } else {
                node.children = new ArrayList<>();
            }
            nodes.add(node);
        }
        return nodes;
    }

    private List<RuleTask> synthesizeInDepthSubtasks(String taskDescription, double multiplier) {
        List<RuleTask> subtasks = new ArrayList<>();
        String lower = (taskDescription == null) ? "" : taskDescription.toLowerCase(Locale.ROOT);

        if (lower.contains("learn") || lower.contains("study") || lower.contains("syntax")) {
            subtasks.add(createSynthesizedSubtask("Explore foundations, syntax rules & environment setup for " + taskDescription, "HIGH", 2));
            subtasks.add(createSynthesizedSubtask("Hands-on drills, problem sets & core patterns for " + taskDescription, "HIGH", 3));
            subtasks.add(createSynthesizedSubtask("Review edge cases, error handling & documentation for " + taskDescription, "MEDIUM", 1));
        } else if (lower.contains("build") || lower.contains("implement") || lower.contains("project") || lower.contains("develop")) {
            subtasks.add(createSynthesizedSubtask("Architect component interfaces & data contracts for " + taskDescription, "HIGH", 2));
            subtasks.add(createSynthesizedSubtask("Construct core implementation & feature logic for " + taskDescription, "HIGH", 4));
            subtasks.add(createSynthesizedSubtask("Write automated tests, refactor modules & verify " + taskDescription, "MEDIUM", 2));
        } else if (lower.contains("test") || lower.contains("revise") || lower.contains("review") || lower.contains("audit")) {
            subtasks.add(createSynthesizedSubtask("Conduct systematic validation & defect identification for " + taskDescription, "HIGH", 2));
            subtasks.add(createSynthesizedSubtask("Apply fixes, benchmark performance & polish deliverables for " + taskDescription, "MEDIUM", 1));
        } else {
            subtasks.add(createSynthesizedSubtask("Analyze requirements, scope & prerequisite setup for " + taskDescription, "HIGH", 1));
            subtasks.add(createSynthesizedSubtask("Execute primary milestones & core deliverables for " + taskDescription, "HIGH", 3));
            subtasks.add(createSynthesizedSubtask("Validate outcomes, address blockers & document results for " + taskDescription, "MEDIUM", 1));
        }
        return subtasks;
    }

    private RuleTask createSynthesizedSubtask(String description, String priority, int estimatedDays) {
        RuleTask t = new RuleTask();
        t.description = description;
        t.priority = priority;
        t.estimatedDays = estimatedDays;
        t.subtasks = new ArrayList<>();
        return t;
    }

    /**
     * Walks the tree assigning:
     *  - orderIndex (position among siblings)
     *  - dependsOn (each task depends on its previous sibling — simple
     *    sequential dependency chain, e.g. "Learn CSS" depends on
     *    "Learn HTML" being done first)
     *  - score (priority weight + earlier-position bonus)
     */
    private void assignOrderScoreAndDependencies(List<TaskBuildResult> siblings, int depth) {
        TaskBuildResult previous = null;
        for (int i = 0; i < siblings.size(); i++) {
            TaskBuildResult node = siblings.get(i);
            node.orderIndex = i;
            node.dependsOnPrevious = previous; // resolved to a real id after persistence
            node.score = computeScore(node.priority, i);
            previous = node;

            if (!node.children.isEmpty()) {
                assignOrderScoreAndDependencies(node.children, depth + 1);
            }
        }
    }

    private double computeScore(String priority, int positionIndex) {
        double priorityWeight;
        switch (priority.toUpperCase(Locale.ROOT)) {
            case "HIGH":
                priorityWeight = 30;
                break;
            case "LOW":
                priorityWeight = 10;
                break;
            default:
                priorityWeight = 20;
        }
        double positionBonus = Math.max(0, 10 - positionIndex); // earlier tasks score slightly higher
        return priorityWeight + positionBonus;
    }

    /**
     * Intermediate in-memory tree node produced by the decomposition engine,
     * before it is persisted as a real Task entity (which needs DB-generated ids
     * for parentId / dependsOnTaskId).
     */
    public static class TaskBuildResult {
        public String description;
        public String priority;
        public int estimatedDays;
        public int orderIndex;
        public double score;
        public TaskBuildResult dependsOnPrevious;
        public List<TaskBuildResult> children = new ArrayList<>();

        /** Convenience: converts this node into a persistable Task (without parent/depends ids yet). */
        public Task toEntity(Long goalId) {
            Task task = new Task();
            task.setGoalId(goalId);
            task.setDescription(this.description);
            task.setPriority(this.priority);
            task.setEstimatedDays(this.estimatedDays);
            task.setOrderIndex(this.orderIndex);
            task.setScore(this.score);
            task.setCompleted(false);
            return task;
        }
    }
}

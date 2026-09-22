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

        public DecompositionPlan(List<TaskBuildResult> tasks, String matchType, String matchedKeyword, String explanation) {
            this.tasks = tasks;
            this.matchType = matchType;
            this.matchedKeyword = matchedKeyword;
            this.explanation = explanation;
        }
    }

    public List<TaskBuildResult> decompose(String goalTitle, String difficulty) {
        return decomposePlan(goalTitle, difficulty).tasks;
    }

    public DecompositionPlan decomposePlan(String goalTitle, String difficulty) {
        String cleaned = cleanGoal(goalTitle);
        double multiplier = difficultyMultiplier(difficulty);

        String matchType = "DEFAULT";
        String matchedKeyword = "general-plan";
        String explanation = "Applied generalized heuristic breakdown template";

        // 1. Topic Match
        MatchedRule topicMatch = findTopicMatch(cleaned);
        List<RuleTask> matchedTasks = null;

        if (topicMatch != null) {
            matchedTasks = topicMatch.tasks;
            matchType = "TOPIC";
            matchedKeyword = topicMatch.keyword;
            explanation = "Matched specialized topic curriculum for '" + topicMatch.keyword + "'";
        }

        // 2. Verb Template Match
        if (matchedTasks == null) {
            MatchedVerb verbMatch = findVerbMatch(cleaned, goalTitle);
            if (verbMatch != null) {
                matchedTasks = verbMatch.tasks;
                matchType = "VERB";
                matchedKeyword = verbMatch.keyword;
                explanation = "Matched action verb template '" + verbMatch.keyword + "' targeting '" + verbMatch.topic + "'";
            }
        }

        // 3. Fallback Default Template
        if (matchedTasks == null) {
            matchedTasks = applyDefault(goalTitle);
            matchType = "DEFAULT";
            matchedKeyword = "default";
            explanation = "Applied 6-phase goal execution framework";
        }

        List<TaskBuildResult> result = buildTree(matchedTasks, multiplier, null);

        // HARD difficulty: add one extra advanced/deep-dive step at the end
        if ("hard".equalsIgnoreCase(difficulty)) {
            TaskBuildResult extra = new TaskBuildResult();
            extra.description = "Deep-dive: explore advanced topics and edge cases";
            extra.priority = "MEDIUM";
            extra.estimatedDays = Math.max(2, (int) Math.round(5 * multiplier));
            extra.children = new ArrayList<>();
            result.add(extra);
        }

        assignOrderScoreAndDependencies(result, 0);
        return new DecompositionPlan(result, matchType, matchedKeyword, explanation);
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

    /** Replaces the {topic} / {goal} placeholder in every task description with the actual phrase. */
    private List<RuleTask> substituteTopic(List<RuleTask> templateTasks, String topic) {
        List<RuleTask> out = new ArrayList<>();
        for (RuleTask t : templateTasks) {
            RuleTask copy = new RuleTask();
            copy.description = t.description
                    .replace("{topic}", topic)
                    .replace("{goal}", topic);
            copy.priority = t.priority;
            copy.estimatedDays = t.estimatedDays;
            copy.subtasks = t.subtasks;
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

    private List<TaskBuildResult> buildTree(List<RuleTask> ruleTasks, double multiplier, TaskBuildResult parent) {
        List<TaskBuildResult> nodes = new ArrayList<>();
        for (RuleTask rt : ruleTasks) {
            TaskBuildResult node = new TaskBuildResult();
            node.description = rt.description;
            node.priority = (rt.priority == null) ? "MEDIUM" : rt.priority.toUpperCase(Locale.ROOT);
            node.estimatedDays = Math.max(1, (int) Math.round(rt.estimatedDays * multiplier));
            node.children = (rt.subtasks == null || rt.subtasks.isEmpty())
                    ? new ArrayList<>()
                    : buildTree(rt.subtasks, multiplier, node);
            nodes.add(node);
        }
        return nodes;
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

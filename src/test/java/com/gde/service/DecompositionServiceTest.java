package com.gde.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecompositionServiceTest {

    private DecompositionService service;

    @BeforeEach
    void setUp() {
        service = new DecompositionService();
        service.loadRules();
    }

    @Test
    @DisplayName("Should match topic rule for 'Learn Web Development' and generate tree")
    void testTopicMatchWebDev() {
        DecompositionService.DecompositionPlan plan = service.decomposePlan("Learn Web Development", "medium");

        assertNotNull(plan);
        assertEquals("TOPIC", plan.matchType);
        assertTrue(plan.matchedKeyword.contains("web development"));
        assertFalse(plan.tasks.isEmpty());

        // Check top-level tasks
        List<String> descriptions = plan.tasks.stream().map(t -> t.description).toList();
        assertTrue(descriptions.contains("Learn HTML"));
        assertTrue(descriptions.contains("Learn CSS"));
        assertTrue(descriptions.contains("Learn JavaScript"));
        assertTrue(descriptions.contains("Build Projects"));

        // Check subtasks for "Build Projects"
        DecompositionService.TaskBuildResult buildProjects = plan.tasks.stream()
                .filter(t -> t.description.equals("Build Projects"))
                .findFirst()
                .orElse(null);

        assertNotNull(buildProjects);
        assertFalse(buildProjects.children.isEmpty(), "Build Projects should have nested subtasks");
        assertTrue(buildProjects.children.stream().anyMatch(c -> c.description.contains("portfolio website")));
    }

    @Test
    @DisplayName("Should handle punctuation and casing gracefully (e.g. 'Learn Python!')")
    void testTopicMatchWithPunctuation() {
        DecompositionService.DecompositionPlan plan = service.decomposePlan("Learn Python!", "medium");

        assertNotNull(plan);
        assertEquals("TOPIC", plan.matchType);
        assertEquals("python", plan.matchedKeyword);
    }

    @Test
    @DisplayName("Should match verb template when topic is not in predefined catalog (e.g. 'Build a Drone')")
    void testVerbMatchBuild() {
        DecompositionService.DecompositionPlan plan = service.decomposePlan("Build a Drone", "medium");

        assertNotNull(plan);
        assertEquals("VERB", plan.matchType);
        assertEquals("build", plan.matchedKeyword);
        assertTrue(plan.explanation.contains("drone"));
        assertTrue(plan.tasks.stream().anyMatch(t -> t.description.toLowerCase().contains("drone")));
    }

    @Test
    @DisplayName("Should fall back to default template for generic or unrecognized goals")
    void testDefaultTemplateFallback() {
        DecompositionService.DecompositionPlan plan = service.decomposePlan("Something Unique 9999", "medium");

        assertNotNull(plan);
        assertEquals("DEFAULT", plan.matchType);
        assertEquals("default", plan.matchedKeyword);
        assertEquals(6, plan.tasks.size());
    }

    @Test
    @DisplayName("Difficulty level should scale estimated days and hard mode should append deep-dive step")
    void testDifficultyScaling() {
        DecompositionService.DecompositionPlan easyPlan = service.decomposePlan("Learn Python", "easy");
        DecompositionService.DecompositionPlan hardPlan = service.decomposePlan("Learn Python", "hard");

        int easyDays = easyPlan.tasks.stream().mapToInt(t -> t.estimatedDays).sum();
        int hardDays = hardPlan.tasks.stream().mapToInt(t -> t.estimatedDays).sum();

        assertTrue(hardDays > easyDays, "Hard plan should have higher estimated days than easy plan");
        assertTrue(hardPlan.tasks.stream().anyMatch(t -> t.description.toLowerCase().contains("deep-dive")),
                "Hard difficulty should add an extra deep-dive task");
    }

    @Test
    @DisplayName("Sequential dependencies and scores should be assigned properly")
    void testDependencyAndScoreAssignment() {
        DecompositionService.DecompositionPlan plan = service.decomposePlan("Learn Web Development", "medium");

        // First task should not have dependsOnPrevious
        assertNull(plan.tasks.get(0).dependsOnPrevious);
        // Second task depends on first task
        assertEquals(plan.tasks.get(0), plan.tasks.get(1).dependsOnPrevious);

        // Scores should be computed (> 0)
        for (DecompositionService.TaskBuildResult t : plan.tasks) {
            assertTrue(t.score > 0, "Task score should be positive");
        }
    }

    @Test
    @DisplayName("In-depth decomposition should generate granular multi-level subtasks for topics, verbs, and defaults")
    void testInDepthPlanHasSubtasks() {
        DecompositionService.DecompositionPlan topicPlan = service.decomposePlan("Learn Python", "medium", "in-depth");
        assertNotNull(topicPlan);
        assertEquals("in-depth", topicPlan.depth);
        assertTrue(topicPlan.totalSubtasks > 0, "In-depth topic plan must contain granular subtasks");
        assertTrue(topicPlan.tasks.stream().allMatch(t -> !t.children.isEmpty()),
                "Every top-level phase in an in-depth topic plan should have detailed subtasks");

        DecompositionService.DecompositionPlan verbPlan = service.decomposePlan("Build an Autonomous Drone", "medium", "in-depth");
        assertNotNull(verbPlan);
        assertTrue(verbPlan.totalSubtasks > 0, "In-depth verb plan must contain granular subtasks");
        // Verify subtask topic substitution worked
        boolean subtaskHasDrone = verbPlan.tasks.stream()
                .flatMap(t -> t.children.stream())
                .anyMatch(c -> c.description.toLowerCase().contains("drone"));
        assertTrue(subtaskHasDrone, "Nested subtasks should dynamically inherit and substitute the topic name");

        DecompositionService.DecompositionPlan defaultPlan = service.decomposePlan("Unusual Goal XYZ 123", "medium", "in-depth");
        assertNotNull(defaultPlan);
        assertTrue(defaultPlan.totalSubtasks > 0, "Default fallback in-depth plan must contain granular subtasks");
    }

    @Test
    @DisplayName("Standard mode should return only high-level phase milestones without nested subtasks")
    void testStandardModeOmission() {
        DecompositionService.DecompositionPlan standardPlan = service.decomposePlan("Learn Python", "medium", "standard");
        assertNotNull(standardPlan);
        assertEquals("standard", standardPlan.depth);
        assertEquals(0, standardPlan.totalSubtasks, "Standard mode should omit nested subtasks");
        for (DecompositionService.TaskBuildResult t : standardPlan.tasks) {
            assertTrue(t.children.isEmpty(), "Tasks in standard mode should not have children");
        }
    }
}

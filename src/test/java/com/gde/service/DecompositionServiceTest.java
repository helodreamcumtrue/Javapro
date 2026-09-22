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
}

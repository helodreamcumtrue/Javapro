package com.gde.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /goal should submit goal and return 201 with decomposed tasks")
    void testSubmitGoalAtRoot() throws Exception {
        String jsonPayload = """
                {
                    "title": "Learn Web Development",
                    "difficulty": "medium"
                }
                """;

        mockMvc.perform(post("/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalId").isNumber())
                .andExpect(jsonPath("$.title", is("Learn Web Development")))
                .andExpect(jsonPath("$.difficulty", is("medium")))
                .andExpect(jsonPath("$.tasks", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.matchType", is("TOPIC")))
                .andExpect(jsonPath("$.tasks[0].description", is("Learn HTML")));
    }

    @Test
    @DisplayName("POST /api/goal should also work for API compatibility")
    void testSubmitGoalAtApiPrefix() throws Exception {
        String jsonPayload = """
                {
                    "title": "Build a Mobile App",
                    "difficulty": "easy"
                }
                """;

        mockMvc.perform(post("/api/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Build a Mobile App")))
                .andExpect(jsonPath("$.matchType", is("TOPIC")));
    }

    @Test
    @DisplayName("GET /tasks/{goalId} should retrieve previously decomposed goal")
    void testGetTasksForGoal() throws Exception {
        // Create first
        String res = mockMvc.perform(post("/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn Python\",\"difficulty\":\"medium\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract goalId using simple regex
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"goalId\":(\\d+)").matcher(res);
        assertTrue(m.find(), "goalId should be present in response");
        String goalId = m.group(1);

        mockMvc.perform(get("/tasks/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId", is(Integer.parseInt(goalId))))
                .andExpect(jsonPath("$.title", is("Learn Python")))
                .andExpect(jsonPath("$.tasks", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /goals should return list of all goals")
    void testGetAllGoals() throws Exception {
        mockMvc.perform(get("/goals"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("PATCH /tasks/{taskId}/toggle should invert task completed status")
    void testToggleTaskStatus() throws Exception {
        // Create a goal first to get a real task ID
        String res = mockMvc.perform(post("/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn Java\",\"difficulty\":\"medium\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(res);
        assertTrue(m.find(), "task id should be present");
        String taskId = m.group(1);

        // Toggle to true
        mockMvc.perform(patch("/tasks/" + taskId + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId", is(Integer.parseInt(taskId))))
                .andExpect(jsonPath("$.completed", is(true)));

        // Toggle back to false
        mockMvc.perform(patch("/tasks/" + taskId + "/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    @DisplayName("POST /goal with blank title should return 400 Bad Request")
    void testBlankGoalValidation() throws Exception {
        mockMvc.perform(post("/goal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"difficulty\":\"medium\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("empty")));
    }

    private static void assertTrue(boolean condition, String msg) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, msg);
    }
}

package com.gde.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Incoming JSON body for POST /api/goal
 * Example: { "title": "Learn Web Development", "difficulty": "medium" }
 */
public class GoalRequest {

    @NotBlank(message = "Goal title must not be empty")
    private String title;

    /** easy | medium | hard  (defaults to "medium" if omitted) */
    private String difficulty = "medium";

    public GoalRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}

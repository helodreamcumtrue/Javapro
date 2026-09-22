package com.gde.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned by both POST /api/goal and GET /api/tasks/{goalId}.
 */
public class GoalResponse {

    private Long goalId;
    private String title;
    private String difficulty;
    private LocalDateTime createdAt;
    private int totalEstimatedDays;
    private int totalTaskCount;
    private int completedTaskCount;
    private String matchType;
    private String matchedKeyword;
    private String explanation;
    private List<TaskNode> tasks;

    public GoalResponse() {
    }

    public GoalResponse(Long goalId, String title, String difficulty,
                         LocalDateTime createdAt, int totalEstimatedDays,
                         int totalTaskCount, int completedTaskCount,
                         String matchType, String matchedKeyword, String explanation,
                         List<TaskNode> tasks) {
        this.goalId = goalId;
        this.title = title;
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.totalEstimatedDays = totalEstimatedDays;
        this.totalTaskCount = totalTaskCount;
        this.completedTaskCount = completedTaskCount;
        this.matchType = matchType;
        this.matchedKeyword = matchedKeyword;
        this.explanation = explanation;
        this.tasks = tasks;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getTotalEstimatedDays() {
        return totalEstimatedDays;
    }

    public void setTotalEstimatedDays(int totalEstimatedDays) {
        this.totalEstimatedDays = totalEstimatedDays;
    }

    public int getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(int totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public void setCompletedTaskCount(int completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchedKeyword() {
        return matchedKeyword;
    }

    public void setMatchedKeyword(String matchedKeyword) {
        this.matchedKeyword = matchedKeyword;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public List<TaskNode> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskNode> tasks) {
        this.tasks = tasks;
    }
}

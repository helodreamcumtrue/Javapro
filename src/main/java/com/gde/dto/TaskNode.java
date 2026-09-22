package com.gde.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree-shaped representation of a Task returned to the frontend.
 * Children are nested so the UI can render an actual hierarchy
 * instead of a flat list.
 */
public class TaskNode {

    private Long id;
    private String description;
    private String priority;
    private int estimatedDays;
    private Long dependsOnTaskId;
    private double score;
    private boolean completed;
    private List<TaskNode> children = new ArrayList<>();

    public TaskNode() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public void setEstimatedDays(int estimatedDays) {
        this.estimatedDays = estimatedDays;
    }

    public Long getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public void setDependsOnTaskId(Long dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<TaskNode> getChildren() {
        return children;
    }

    public void setChildren(List<TaskNode> children) {
        this.children = children;
    }
}

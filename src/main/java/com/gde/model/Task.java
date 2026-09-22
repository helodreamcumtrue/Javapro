package com.gde.model;

import jakarta.persistence.*;

/**
 * Represents a single decomposed task belonging to a Goal.
 * Tasks form a tree via parentId (self-reference, not a JPA relation,
 * kept simple so the tree can be assembled flexibly in the service layer).
 *
 * Table: Tasks(id, description, parent_id, goal_id, priority, order_index,
 *              estimated_days, depends_on_task_id, score, completed)
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    /** Null for top-level tasks, otherwise points to the parent Task's id */
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    /** HIGH / MEDIUM / LOW */
    @Column(nullable = false)
    private String priority;

    /** Position among siblings — used to derive sequential dependency order */
    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /** Rough time estimate in days, adjusted by goal difficulty */
    @Column(name = "estimated_days", nullable = false)
    private int estimatedDays;

    /** Id of the task (usually the previous sibling) that must be completed first. Null if none. */
    @Column(name = "depends_on_task_id")
    private Long dependsOnTaskId;

    /** Simple computed importance score (higher = more important / do first) */
    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private boolean completed;

    public Task() {
    }

    // ----- Getters & Setters -----
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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
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
}

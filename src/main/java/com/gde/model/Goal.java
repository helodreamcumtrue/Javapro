package com.gde.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a user-defined goal, e.g. "Learn Web Development".
 * Table: Goals(id, title, difficulty, created_at)
 */
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** EASY / MEDIUM / HARD - affects timeline estimation */
    @Column(nullable = false)
    private String difficulty;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "match_type")
    private String matchType;

    @Column(name = "matched_keyword")
    private String matchedKeyword;

    @Column(name = "explanation")
    private String explanation;

    public Goal() {
    }

    public Goal(String title, String difficulty) {
        this.title = title;
        this.difficulty = difficulty;
        this.createdAt = LocalDateTime.now();
    }

    public Goal(String title, String difficulty, String matchType, String matchedKeyword, String explanation) {
        this.title = title;
        this.difficulty = difficulty;
        this.createdAt = LocalDateTime.now();
        this.matchType = matchType;
        this.matchedKeyword = matchedKeyword;
        this.explanation = explanation;
    }

    // ----- Getters & Setters -----
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}

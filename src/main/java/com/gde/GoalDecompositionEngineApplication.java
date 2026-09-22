package com.gde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Goal Decomposition Engine.
 *
 * This is a 100% local, offline, rule-based system:
 *  - No external AI APIs are called.
 *  - No paid cloud services are used.
 *  - Decomposition logic is pure Java if-else / keyword matching (see DecompositionService).
 *  - Data is persisted in a local SQLite file (goal_decomposition.db).
 */
@SpringBootApplication
public class GoalDecompositionEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoalDecompositionEngineApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" Goal Decomposition Engine started successfully!");
        System.out.println(" Open http://localhost:8080 in your browser.");
        System.out.println("=================================================");
    }
}

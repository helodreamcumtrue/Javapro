package com.gde.controller;

import com.gde.dto.GoalRequest;
import com.gde.dto.GoalResponse;
import com.gde.model.Goal;
import com.gde.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the Goal Decomposition Engine.
 *
 *   POST /api/goal            -> submit a goal, get back the decomposed task tree
 *   GET  /api/tasks/{goalId}  -> fetch the decomposed tasks for an existing goal
 *   GET  /api/goals           -> list all previously submitted goals (for the UI sidebar)
 */
@RestController
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping({"/goal", "/api/goal"})
    public ResponseEntity<GoalResponse> submitGoal(@Valid @RequestBody GoalRequest request) {
        GoalResponse response = goalService.createGoalAndDecompose(request.getTitle(), request.getDifficulty());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/tasks/{goalId}", "/api/tasks/{goalId}"})
    public ResponseEntity<GoalResponse> getTasks(@PathVariable Long goalId) {
        GoalResponse response = goalService.getGoalWithTasks(goalId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/goals", "/api/goals"})
    public ResponseEntity<List<Goal>> getAllGoals() {
        return ResponseEntity.ok(goalService.getAllGoals());
    }

    @PatchMapping({"/tasks/{taskId}/toggle", "/api/tasks/{taskId}/toggle"})
    public ResponseEntity<Map<String, Object>> toggleTask(@PathVariable Long taskId) {
        com.gde.model.Task task = goalService.toggleTask(taskId);
        return ResponseEntity.ok(Map.of(
                "taskId", task.getId(),
                "completed", task.isCompleted()
        ));
    }

    @DeleteMapping({"/goals/{goalId}", "/api/goals/{goalId}"})
    public ResponseEntity<Map<String, String>> deleteGoal(@PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.ok(Map.of("message", "Goal " + goalId + " deleted successfully"));
    }

    // ----- Simple error handling so the frontend gets a readable message -----

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid request"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }
}

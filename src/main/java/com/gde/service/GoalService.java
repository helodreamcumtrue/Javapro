package com.gde.service;

import com.gde.dto.GoalResponse;
import com.gde.dto.TaskNode;
import com.gde.model.Goal;
import com.gde.model.Task;
import com.gde.repository.GoalRepository;
import com.gde.repository.TaskRepository;
import com.gde.service.DecompositionService.TaskBuildResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Orchestrates: Goal creation -> rule-based decomposition -> persistence -> tree response.
 */
@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final DecompositionService decompositionService;

    public GoalService(GoalRepository goalRepository,
                        TaskRepository taskRepository,
                        DecompositionService decompositionService) {
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.decompositionService = decompositionService;
    }

    /**
     * Creates a new Goal, runs the rule-based engine, and persists the
     * resulting task tree with real DB-generated parent/dependency ids.
     */
    @Transactional
    public GoalResponse createGoalAndDecompose(String title, String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            difficulty = "medium";
        }

        DecompositionService.DecompositionPlan plan = decompositionService.decomposePlan(title, difficulty);

        Goal goal = new Goal(title.trim(), difficulty.toLowerCase(Locale.ROOT),
                plan.matchType, plan.matchedKeyword, plan.explanation);
        goal = goalRepository.save(goal);

        // Persist top-down so children can reference the real (saved) parent id,
        // and each sibling can reference the real id of its predecessor for dependsOnTaskId.
        persistTree(plan.tasks, goal.getId(), null);

        return getGoalWithTasks(goal.getId());
    }

    /** Recursively saves a tree of TaskBuildResult nodes, wiring real DB ids for parent/dependency links. */
    private void persistTree(List<TaskBuildResult> nodes, Long goalId, Long parentId) {
        Map<TaskBuildResult, Long> savedIds = new IdentityHashMap<>();

        for (TaskBuildResult node : nodes) {
            Task entity = node.toEntity(goalId);
            entity.setParentId(parentId);

            if (node.dependsOnPrevious != null) {
                Long dependsOnId = savedIds.get(node.dependsOnPrevious);
                entity.setDependsOnTaskId(dependsOnId);
            }

            Task saved = taskRepository.save(entity);
            savedIds.put(node, saved.getId());

            if (!node.children.isEmpty()) {
                persistTree(node.children, goalId, saved.getId());
            }
        }
    }

    /** Fetches an existing goal + rebuilds its task tree from flat DB rows. */
    @Transactional(readOnly = true)
    public GoalResponse getGoalWithTasks(Long goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new NoSuchElementException("Goal not found with id: " + goalId));

        List<Task> flatTasks = taskRepository.findByGoalIdOrderByOrderIndexAsc(goalId);
        List<TaskNode> tree = buildTreeFromFlatList(flatTasks, null);

        int totalDays = flatTasks.stream()
                .filter(t -> t.getParentId() == null) // count only top-level so nested project sub-days aren't double counted
                .mapToInt(Task::getEstimatedDays)
                .sum();

        int totalTasks = flatTasks.size();
        int completedTasks = (int) flatTasks.stream().filter(Task::isCompleted).count();

        return new GoalResponse(goal.getId(), goal.getTitle(), goal.getDifficulty(),
                goal.getCreatedAt(), totalDays, totalTasks, completedTasks,
                goal.getMatchType(), goal.getMatchedKeyword(), goal.getExplanation(), tree);
    }

    @Transactional
    public Task toggleTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found with id: " + taskId));
        task.setCompleted(!task.isCompleted());
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteGoal(Long goalId) {
        if (!goalRepository.existsById(goalId)) {
            throw new NoSuchElementException("Goal not found with id: " + goalId);
        }
        taskRepository.deleteByGoalId(goalId);
        goalRepository.deleteById(goalId);
    }

    @Transactional(readOnly = true)
    public List<Goal> getAllGoals() {
        List<Goal> goals = goalRepository.findAll();
        goals.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return goals;
    }

    /** Converts a flat Task list into a nested TaskNode tree, keyed by parentId. */
    private List<TaskNode> buildTreeFromFlatList(List<Task> flatTasks, Long parentId) {
        List<TaskNode> result = new ArrayList<>();
        for (Task t : flatTasks) {
            boolean matchesParent = (parentId == null) ? t.getParentId() == null : parentId.equals(t.getParentId());
            if (matchesParent) {
                TaskNode node = new TaskNode();
                node.setId(t.getId());
                node.setDescription(t.getDescription());
                node.setPriority(t.getPriority());
                node.setEstimatedDays(t.getEstimatedDays());
                node.setDependsOnTaskId(t.getDependsOnTaskId());
                node.setScore(t.getScore());
                node.setCompleted(t.isCompleted());
                node.setChildren(buildTreeFromFlatList(flatTasks, t.getId()));
                result.add(node);
            }
        }
        return result;
    }
}

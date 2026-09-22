package com.gde.repository;

import com.gde.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for Task entities, with a couple of derived query methods
 * used to rebuild the task tree for a given goal.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGoalIdOrderByOrderIndexAsc(Long goalId);

    void deleteByGoalId(Long goalId);
}

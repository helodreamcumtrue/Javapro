package com.gde.repository;

import com.gde.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Standard Spring Data JPA repository for Goal entities.
 * Gives us save(), findById(), findAll() etc. for free.
 */
public interface GoalRepository extends JpaRepository<Goal, Long> {
}

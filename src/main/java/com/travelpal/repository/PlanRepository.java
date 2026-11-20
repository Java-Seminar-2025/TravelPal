package com.travelpal.repository;

import com.travelpal.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByOwnerId(Long ownerId);
    List<Plan> findByCityIgnoreCase(String city);
    List<Plan> findByVisibility(Plan.Visibility visibility);
}
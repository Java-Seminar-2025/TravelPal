package com.travelpal.repository;

import com.travelpal.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByOwnerId(UUID ownerId);
    List<Plan> findByCityIgnoreCase(String city);
    List<Plan> findByVisibility(Plan.Visibility visibility);
}
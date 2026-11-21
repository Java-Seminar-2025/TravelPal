package com.travelpal.repository;

import com.travelpal.entity.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = {"owner"})
    List<Plan> findByCityIgnoreCase(String city);

    List<Plan> findByVisibility(Plan.Visibility visibility);

    @EntityGraph(attributePaths = {"owner"})
    List<Plan> findByVisibilityOrderByCreatedAtDesc(Plan.Visibility visibility);

    List<Plan> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    @EntityGraph(attributePaths = {"owner", "planItems"})
    Optional<Plan> findWithDetailsById(UUID id);
}
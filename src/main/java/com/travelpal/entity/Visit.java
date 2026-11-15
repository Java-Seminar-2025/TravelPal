package com.travelpal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visits", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"adventure_id", "plan_item_id"})
})
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adventure_id", nullable = false)
    private Adventure adventure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_item_id", nullable = false)
    private PlanItem planItem;

    @Column(name = "visited_at", nullable = false, updatable = false)
    private LocalDateTime visitedAt = LocalDateTime.now();

    // Constructors, Getters, and Setters
    public Visit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Adventure getAdventure() { return adventure; }
    public void setAdventure(Adventure adventure) { this.adventure = adventure; }

    public PlanItem getPlanItem() { return planItem; }
    public void setPlanItem(PlanItem planItem) { this.planItem = planItem; }

    public LocalDateTime getVisitedAt() { return visitedAt; }
    public void setVisitedAt(LocalDateTime visitedAt) { this.visitedAt = visitedAt; }
}

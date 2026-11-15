package com.travelpal.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "plan_items")
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "tripadvisor_location_id", nullable = false, length = 30)
    private String tripadvisorLocationId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "planItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Visit> visits;

    // Constructors, Getters, and Setters
    public PlanItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public String getTripadvisorLocationId() { return tripadvisorLocationId; }
    public void setTripadvisorLocationId(String tripadvisorLocationId) { this.tripadvisorLocationId = tripadvisorLocationId; }

    public Integer getDayNumber() { return dayNumber; }
    public void setDayNumber(Integer dayNumber) { this.dayNumber = dayNumber; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

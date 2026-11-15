package com.travelpal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Entity
@Table(name = "plan_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
}

package com.travelpal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plan_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false, length = 200)
    private String name;

    /** Map position. Null only for old items created before the map existed. */
    private Double latitude;
    private Double longitude;

    /** OpenStreetMap id (e.g. "w43768260"), or null for a spot the user clicked on the map. */
    @Column(name = "osm_id", length = 40)
    private String osmId;

    @Column(name = "tripadvisor_location_id", length = 30)
    private String tripadvisorLocationId;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(length = 500)
    private String note;

    @OneToMany(mappedBy = "planItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Visit> visits = new ArrayList<>();
}

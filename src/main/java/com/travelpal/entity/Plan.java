package com.travelpal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copied_from_id")
    private Plan copiedFrom;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 100)
    private String city;

    /** Where the city is (picked from OpenStreetMap). Null for plans made before the city picker existed. */
    @Column(name = "city_latitude")
    private Double cityLatitude;

    @Column(name = "city_longitude")
    private Double cityLongitude;

    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "ai_generated", nullable = false)
    private Boolean aiGenerated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanItem> planItems = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Adventure> adventures = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings = new ArrayList<>();

    public enum Visibility { PRIVATE, PUBLIC }
}

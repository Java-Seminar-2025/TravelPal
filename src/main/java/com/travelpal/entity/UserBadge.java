package com.travelpal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBadge {

    @EmbeddedId
    private UserBadgeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "badge_code", insertable = false, updatable = false, length = 40)
    private String badgeCode;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt = LocalDateTime.now();
}
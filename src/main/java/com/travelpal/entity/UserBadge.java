package com.travelpal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_badges")
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

    // Constructors, Getters, and Setters
    public UserBadge() {}

    public UserBadgeId getId() { return id; }
    public void setId(UserBadgeId id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getBadgeCode() { return badgeCode; }

    public LocalDateTime getAwardedAt() { return awardedAt; }
    public void setAwardedAt(LocalDateTime awardedAt) { this.awardedAt = awardedAt; }
}

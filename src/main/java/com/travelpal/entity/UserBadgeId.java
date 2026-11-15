package com.travelpal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserBadgeId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "badge_code", length = 40)
    private String badgeCode;

    public UserBadgeId() {}

    public UserBadgeId(Long userId, String badgeCode) {
        this.userId = userId;
        this.badgeCode = badgeCode;
    }

    // hashCode and equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBadgeId)) return false;
        UserBadgeId that = (UserBadgeId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(badgeCode, that.badgeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, badgeCode);
    }
}

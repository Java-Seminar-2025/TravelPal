package com.travelpal.repository;

import com.travelpal.entity.Plan;
import com.travelpal.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlanRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlanRepository planRepository;

    @Test
    void whenFindByCityIgnoreCase_thenReturnPlans() {
        // given
        User owner = new User();
        owner.setUsername("planner");
        owner.setEmail("planner@travelpal.com");
        owner.setPasswordHash("hash");
        entityManager.persist(owner);

        Plan plan = new Plan();
        plan.setOwner(owner);
        plan.setTitle("Barcelona Getaway");
        plan.setCity("Barcelona"); // Use a unique city not present in seed data
        plan.setDays(4);
        plan.setVisibility("PUBLIC");
        plan.setAiGenerated(false);
        entityManager.persistAndFlush(plan);

        // when
        List<Plan> plans = planRepository.findByCityIgnoreCase("barcelona");

        // then
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getTitle()).isEqualTo("Barcelona Getaway");
    }
}
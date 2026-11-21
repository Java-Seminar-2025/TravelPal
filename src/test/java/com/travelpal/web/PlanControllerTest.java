package com.travelpal.web;

import com.travelpal.entity.Plan;
import com.travelpal.entity.User;
import com.travelpal.repository.PlanRepository;
import com.travelpal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PlanRepository planRepository;

    @Autowired
    UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setUsername("explorer");
        owner.setEmail("explorer@example.com");
        owner.setPasswordHash("{bcrypt}$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
        owner.setRole(User.Role.USER);
        userRepository.save(owner);
    }

    @Test
    void anonymousUserIsRedirectedToLoginOnPlans() throws Exception {
        mockMvc.perform(get("/plans"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedUserCanCreatePlanAndViewDetail() throws Exception {
        mockMvc.perform(post("/plans").with(csrf())
                        .with(user("explorer").roles("USER"))
                        .param("title", "Split Weekend")
                        .param("city", "Split")
                        .param("days", "3")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().is3xxRedirection());

        Plan created = planRepository.findByOwnerId(owner.getId()).get(0);
        assertThat(created.getTitle()).isEqualTo("Split Weekend");
        assertThat(created.getCity()).isEqualTo("Split");

        mockMvc.perform(get("/plans/" + created.getId())
                        .with(user("explorer").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("plans/detail"));
    }

    @Test
    void createPlanRejectsInvalidForm() throws Exception {
        mockMvc.perform(post("/plans").with(csrf())
                        .with(user("explorer").roles("USER"))
                        .param("title", "")
                        .param("city", "")
                        .param("days", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("plans/form"))
                .andExpect(model().attributeHasFieldErrors("planForm", "title", "city", "days"));
    }
}
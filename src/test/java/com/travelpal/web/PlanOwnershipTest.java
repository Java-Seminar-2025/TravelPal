package com.travelpal.web;

import com.travelpal.entity.Plan;
import com.travelpal.entity.PlanItem;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Who may do what with a plan:
 * owner -> view, edit, delete; other users -> only view PUBLIC plans; admin -> everything.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanOwnershipTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PlanRepository planRepository;
    @Autowired
    UserRepository userRepository;

    private final RequestPostProcessor owner = user("explorer").roles("USER");
    private final RequestPostProcessor stranger = user("stranger").roles("USER");
    private final RequestPostProcessor admin = user("boss").roles("ADMIN");

    private User explorer;
    private Plan publicPlan;
    private Plan privatePlan;

    @BeforeEach
    void setUp() {
        explorer = saveUser("explorer", User.Role.USER);
        saveUser("stranger", User.Role.USER);
        saveUser("boss", User.Role.ADMIN);
        publicPlan = savePlan("Split Weekend", Plan.Visibility.PUBLIC);
        privatePlan = savePlan("Secret Trip", Plan.Visibility.PRIVATE);
    }

    // --- other users ---

    @Test
    void otherUserCanViewPublicPlanWithoutEditButtons() throws Exception {
        mockMvc.perform(get("/plans/" + publicPlan.getId()).with(stranger))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canModify", false));
    }

    @Test
    void otherUserCannotViewPrivatePlan() throws Exception {
        mockMvc.perform(get("/plans/" + privatePlan.getId()).with(stranger))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherUserCannotChangeOrDeletePlan() throws Exception {
        String url = "/plans/" + publicPlan.getId();
        UUID itemId = publicPlan.getPlanItems().get(0).getId();

        mockMvc.perform(get(url + "/edit").with(stranger)).andExpect(status().isForbidden());
        mockMvc.perform(editRequest(url, "Hacked").with(stranger)).andExpect(status().isForbidden());
        mockMvc.perform(post(url + "/items").with(csrf()).with(stranger)
                        .param("tripadvisorLocationId", "999").param("dayNumber", "1").param("orderIndex", "2"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(url + "/items/" + itemId + "/delete").with(csrf()).with(stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(url + "/delete").with(csrf()).with(stranger)).andExpect(status().isForbidden());

        Plan unchanged = planRepository.findById(publicPlan.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Split Weekend");
        assertThat(unchanged.getPlanItems()).hasSize(1);
    }

    // --- owner ---

    @Test
    void newPlanAlwaysBelongsToLoggedInUser() throws Exception {
        mockMvc.perform(post("/plans").with(csrf()).with(stranger)
                        .param("title", "Zagreb Day").param("city", "Zagreb").param("days", "1")
                        .param("visibility", "PRIVATE")
                        .param("owner", explorer.getId().toString())      // attempts to pick another owner
                        .param("ownerId", explorer.getId().toString()))   // are ignored
                .andExpect(status().is3xxRedirection());

        Plan created = planRepository.findAll().stream()
                .filter(p -> p.getTitle().equals("Zagreb Day")).findFirst().orElseThrow();
        assertThat(created.getOwner().getUsername()).isEqualTo("stranger");
    }

    @Test
    void ownerCanEditPlan() throws Exception {
        mockMvc.perform(editRequest("/plans/" + privatePlan.getId(), "Secret Trip v2").with(owner))
                .andExpect(redirectedUrl("/plans/" + privatePlan.getId()));

        assertThat(planRepository.findById(privatePlan.getId()).orElseThrow().getTitle()).isEqualTo("Secret Trip v2");
    }

    @Test
    void ownerCanRemoveItemAndDeletePlan() throws Exception {
        UUID itemId = publicPlan.getPlanItems().get(0).getId();
        mockMvc.perform(post("/plans/" + publicPlan.getId() + "/items/" + itemId + "/delete").with(csrf()).with(owner))
                .andExpect(status().is3xxRedirection());
        assertThat(publicPlan.getPlanItems()).isEmpty();

        mockMvc.perform(post("/plans/" + publicPlan.getId() + "/delete").with(csrf()).with(owner))
                .andExpect(redirectedUrl("/plans"));
        assertThat(planRepository.findById(publicPlan.getId())).isEmpty();
    }

    @Test
    void editCannotMakePlanShorterThanItsItems() throws Exception {
        PlanItem dayTwo = publicPlan.getPlanItems().get(0);
        dayTwo.setDayNumber(2);

        mockMvc.perform(post("/plans/" + publicPlan.getId() + "/edit").with(csrf()).with(owner)
                        .param("title", "Split Weekend").param("city", "Split").param("days", "1")
                        .param("visibility", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("planForm", "days"));
    }

    // --- admin ---

    @Test
    void adminCanEditAndDeleteAnyonesPlanButOwnerStaysTheSame() throws Exception {
        mockMvc.perform(get("/plans/" + privatePlan.getId()).with(admin))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canModify", true));

        mockMvc.perform(editRequest("/plans/" + privatePlan.getId(), "Moderated title").with(admin))
                .andExpect(status().is3xxRedirection());
        Plan edited = planRepository.findById(privatePlan.getId()).orElseThrow();
        assertThat(edited.getTitle()).isEqualTo("Moderated title");
        assertThat(edited.getOwner().getUsername()).isEqualTo("explorer");

        mockMvc.perform(post("/plans/" + privatePlan.getId() + "/delete").with(csrf()).with(admin))
                .andExpect(redirectedUrl("/plans"));
        assertThat(planRepository.findById(privatePlan.getId())).isEmpty();
    }

    @Test
    void unknownPlanGives404() throws Exception {
        mockMvc.perform(get("/plans/" + UUID.randomUUID()).with(owner))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder editRequest(String planUrl, String title) {
        return post(planUrl + "/edit").with(csrf())
                .param("title", title).param("city", "Split").param("days", "3").param("visibility", "PUBLIC");
    }

    private User saveUser(String username, User.Role role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        u.setPasswordHash("{noop}unused");
        u.setRole(role);
        return userRepository.save(u);
    }

    private Plan savePlan(String title, Plan.Visibility visibility) {
        Plan plan = new Plan();
        plan.setOwner(explorer);
        plan.setTitle(title);
        plan.setCity("Split");
        plan.setDays(3);
        plan.setVisibility(visibility);

        PlanItem item = new PlanItem();
        item.setPlan(plan);
        item.setTripadvisorLocationId("12345");
        item.setDayNumber(1);
        item.setOrderIndex(1);
        plan.getPlanItems().add(item);
        return planRepository.saveAndFlush(plan);
    }
}

package com.travelpal.web;

import com.travelpal.dto.Attraction;
import com.travelpal.dto.CityAttractions;
import com.travelpal.entity.Plan;
import com.travelpal.entity.PlanItem;
import com.travelpal.entity.User;
import com.travelpal.repository.PlanRepository;
import com.travelpal.repository.UserRepository;
import com.travelpal.service.AttractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Adding places from the map to a plan, and the JSON endpoint the map uses. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanMapTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    PlanRepository planRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    AttractionService attractionService;   // fake OpenStreetMap so tests don't need internet

    private Plan plan;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setUsername("explorer");
        owner.setEmail("explorer@example.com");
        owner.setPasswordHash("{noop}unused");
        userRepository.save(owner);

        plan = new Plan();
        plan.setOwner(owner);
        plan.setTitle("Split Weekend");
        plan.setCity("Split");
        plan.setDays(2);
        planRepository.save(plan);
    }

    @Test
    void ownerAddsAttractionFromMapWithItsCoordinates() throws Exception {
        mockMvc.perform(post("/plans/" + plan.getId() + "/items").with(csrf()).with(user("explorer"))
                        .param("name", "Diocletian's Palace")
                        .param("latitude", "43.508505").param("longitude", "16.440249").param("osmId", "w124205148")
                        .param("dayNumber", "1").param("orderIndex", "1"))
                .andExpect(redirectedUrl("/plans/" + plan.getId()));

        PlanItem item = plan.getPlanItems().get(0);
        assertThat(item.getName()).isEqualTo("Diocletian's Palace");
        assertThat(item.getLatitude()).isEqualTo(43.508505);
        assertThat(item.getLongitude()).isEqualTo(16.440249);
        assertThat(item.getOsmId()).isEqualTo("w124205148");
    }

    @Test
    void ownSpotClickedOnMapNeedsNoOsmId() throws Exception {
        mockMvc.perform(post("/plans/" + plan.getId() + "/items").with(csrf()).with(user("explorer"))
                        .param("name", "Our favourite ice cream").param("latitude", "43.5").param("longitude", "16.44")
                        .param("dayNumber", "2").param("orderIndex", "1"))
                .andExpect(status().is3xxRedirection());

        assertThat(plan.getPlanItems().get(0).getOsmId()).isNull();
    }

    @Test
    void placeWithoutCoordinatesIsRejected() throws Exception {
        mockMvc.perform(post("/plans/" + plan.getId() + "/items").with(csrf()).with(user("explorer"))
                        .param("name", "Somewhere").param("dayNumber", "1").param("orderIndex", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("plans/detail"))
                .andExpect(model().attributeHasFieldErrors("itemForm", "latitude", "longitude"));
        assertThat(plan.getPlanItems()).isEmpty();
    }

    @Test
    void attractionsApiReturnsJsonForLoggedInUsers() throws Exception {
        when(attractionService.findAttractions("Split")).thenReturn(new CityAttractions("Split", 43.51, 16.44,
                List.of(new Attraction("w124205148", "Diocletian's Palace", "attraction", 43.508505, 16.440249, true))));

        mockMvc.perform(get("/api/attractions").param("city", "Split").with(user("explorer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Split"))
                .andExpect(jsonPath("$.attractions[0].name").value("Diocletian's Palace"))
                .andExpect(jsonPath("$.attractions[0].osmId").value("w124205148"));
    }

    @Test
    void attractionsApiRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/attractions").param("city", "Split"))
                .andExpect(status().is3xxRedirection());
    }
}

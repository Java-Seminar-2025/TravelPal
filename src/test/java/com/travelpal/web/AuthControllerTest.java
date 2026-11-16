package com.travelpal.web;

import com.travelpal.entity.User;
import com.travelpal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    private void register(String username, String email, String password, String confirm) throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirm));
    }

    @Test
    void registerCreatesUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "newbie")
                        .param("email", "Newbie@Example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123"))
                .andExpect(redirectedUrl("/login?registered"));

        User saved = userRepository.findByUsername("newbie").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("newbie@example.com");
        assertThat(saved.getRole()).isEqualTo(User.Role.USER);
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("secret123");
    }

    @Test
    void registerRejectsTakenUsernameAndMismatchedPasswords() throws Exception {
        register("taken", "taken@example.com", "secret123", "secret123");

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "taken")
                        .param("email", "other@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "different1"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "confirmPassword"));
    }

    @Test
    void registeredUserCanLogInWithUsernameOrEmail() throws Exception {
        register("traveller", "traveller@example.com", "secret123", "secret123");

        mockMvc.perform(formLogin().user("traveller").password("secret123"))
                .andExpect(authenticated().withUsername("traveller").withRoles("USER"));
        mockMvc.perform(formLogin().user("traveller@example.com").password("secret123"))
                .andExpect(authenticated().withUsername("traveller"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        register("traveller", "traveller@example.com", "secret123", "secret123");

        mockMvc.perform(formLogin().user("traveller").password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void loginPageShowsMessagesAfterRegisterAndLogout() throws Exception {
        mockMvc.perform(get("/login").param("registered", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account created")));
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("You have been logged out")));
    }

    @Test
    void adminPageIsOnlyForAdmins() throws Exception {
        mockMvc.perform(get("/admin").with(user("someone").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(user("boss").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }
}

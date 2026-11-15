package com.travelpal.repository;

import com.travelpal.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void whenFindByUsername_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@travelpal.com");
        user.setPasswordHash("hashedpassword");
        user.setRole("USER");
        user.setXp(150);
        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByUsername("testuser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@travelpal.com");
        assertThat(found.get().getXp()).isEqualTo(150);
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("emailuser");
        user.setEmail("unique@travelpal.com");
        user.setPasswordHash("hashedpassword");
        entityManager.persistAndFlush(user);

        // when
        Optional<User> found = userRepository.findByEmail("unique@travelpal.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("emailuser");
    }
}
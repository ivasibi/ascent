package org.ascent.repositories;

import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Container
    public static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8");

    @DynamicPropertySource
    public static void mySqlProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        dynamicPropertyRegistry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        dynamicPropertyRegistry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
    }

    @BeforeEach
    public void beforeEach() {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername("username");
        user.setEmail("username@email.com");
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        user.setDisabled(true);
        user.setRole(Role.USER);
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(Instant.now());

        User user2 = new User();
        user2.setUsername("username2");
        user2.setEmail("username2@email.com");
        user2.setPassword(bCryptPasswordEncoder.encode("password2"));
        user2.setDisabled(false);
        user2.setRole(Role.ADMIN);
        user2.setCreatedOn(Instant.now().minus(10, ChronoUnit.MINUTES));
        user2.setLastLogin(Instant.now());

        userRepository.save(user);
        userRepository.save(user2);
        userRepository.flush();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    @Test
    public void checkIfMySQLContainerIsSetupCorrectly() {
        assertTrue(mySQLContainer.isCreated());
        assertTrue(mySQLContainer.isRunning());
    }

    @ParameterizedTest
    @CsvSource({
            "username",
            "username2"
    })
    public void checkIfCreatedUserExistByUsername(String username) {
        assertTrue(userRepository.existsByUsername(username));
    }

    @ParameterizedTest
    @CsvSource({
            "username3",
            "username4"
    })
    public void checkIfNotCreatedUserExistByUsername(String username) {
        assertFalse(userRepository.existsByUsername(username));
    }

    @ParameterizedTest
    @CsvSource({
            "username@email.com",
            "username2@email.com"
    })
    public void checkIfCreatedUserExistByEmail(String email) {
        assertTrue(userRepository.existsByEmail(email));
    }

    @ParameterizedTest
    @CsvSource({
            "username3@email.com",
            "username4@email.com"
    })
    public void checkIfNotCreatedUserExistByEmail(String email) {
        assertFalse(userRepository.existsByEmail(email));
    }

    @ParameterizedTest
    @CsvSource({
            "username, username@email.com, password, true, USER",
            "username2, username2@email.com, password2, false, ADMIN"
    })
    public void checkIfFindByEmailOfCreatedUserReturnsUser(String username, String email, String password, boolean disabled, Role role) {
        assertAll(
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User user = userRepository.findByEmail(email);
                    assertAll(
                            () -> assertNotNull(user.getId()),
                            () -> assertEquals(username, user.getUsername()),
                            () -> assertEquals(email, user.getEmail()),
                            () -> {
                                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                                assertTrue(bCryptPasswordEncoder.matches(password, user.getPassword()));
                            },
                            () -> assertEquals(disabled, user.isDisabled()),
                            () -> assertEquals(role, user.getRole()),
                            () -> assertNotNull(user.getCreatedOn()),
                            () -> assertNotNull(user.getLastLogin()),
                            () -> assertTrue(user.getCreatedOn().isBefore(user.getLastLogin()))
                    );
                }
        );
    }

    @ParameterizedTest
    @CsvSource({
            "username3@email.com",
            "username4@email.com"
    })
    public void checkIfFindByEmailOfNotCreatedUserReturnsNull(String email) {
        assertNull(userRepository.findByEmail(email));
    }
}
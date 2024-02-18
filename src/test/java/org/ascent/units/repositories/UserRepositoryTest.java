package org.ascent.units.repositories;

import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class UserRepositoryTest extends ContainerEnvironment {

    @Autowired
    private UserRepository userRepository;

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
        user2.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user2.setLastLogin(Instant.now());

        userRepository.save(user);
        userRepository.save(user2);
        userRepository.flush();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    private static Stream<String> checkIfSavedUserExistsByUsername() {
        return Stream.of("username", "username2");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserExistsByUsername(String username) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertTrue(userRepository.existsByUsername(username));
    }

    private static Stream<String> checkIfNotSavedUserExistsByUsername() {
        return Stream.of("username3", "username4");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserExistsByUsername(String username) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertFalse(userRepository.existsByUsername(username));
    }

    private static Stream<String> checkIfSavedUserExistsByEmail() {
        return Stream.of("username@email.com", "username2@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserExistsByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertTrue(userRepository.existsByEmail(email));
    }

    private static Stream<String> checkIfNotSavedUserExistsByEmail() {
        return Stream.of("username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserExistsByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertFalse(userRepository.existsByEmail(email));
    }

    private static Stream<Arguments> checkIfSavedUserIsReturnedByFindByEmail() {
        return Stream.of(
                arguments("username", "username@email.com", "password", true, Role.USER),
                arguments("username2", "username2@email.com", "password2", false, Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserIsReturnedByFindByEmail(String username, String email, String password, boolean disabled, Role role) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

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

    private static Stream<String> checkIfNotSavedUserIsReturnedByFindByEmail() {
        return Stream.of("username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserIsReturnedByFindByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertNull(userRepository.findByEmail(email));
    }
}
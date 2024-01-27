package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.controllers.RegisterController;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.managers.RegisterManager;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class RegisterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisterManager registerManager;

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
        User user = new User();
        user.setUsername("username");
        user.setEmail("username@email.com");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        user.setPassword(bCryptPasswordEncoder.encode("password"));

        user.setRole(Role.USER);
        user.setCreatedOn(Instant.now());

        userRepository.save(user);
        userRepository.flush();

        mockMvc = MockMvcBuilders.standaloneSetup(new RegisterController(registerManager)).build();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    private static Stream<Arguments> callWithNewUserReturnsCreatedAndSuccessAndSavesUser() {
        return Stream.of(
                arguments("username2", "username2@email.com", "password2"),
                arguments("username3", "username3@email.com", "password3"),
                arguments("username4", "username4@email.com", "password4"),
                arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNewUserReturnsCreatedAndSuccessAndSavesUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: success"));

        assertAll(
                () -> assertTrue(userRepository.existsByUsername(username)),
                () -> assertTrue(userRepository.existsByEmail(email)),
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
                            () -> assertFalse(user.isDisabled()),
                            () -> assertEquals(Role.USER, user.getRole()),
                            () -> assertNotNull(user.getCreatedOn()),
                            () -> assertNull(user.getLastLogin())
                    );
                }
        );
    }

    private static Stream<Arguments> callWithUsedUsernameReturnsConflictAndUsernameAlreadyInUseAndDoesNotSaveUser() {
        return Stream.of(
                arguments("username", "username2@email.com", "password"),
                arguments("username", "username2@email.com", "password2"),
                arguments("username", "username3@email.com", "password2"),
                arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithUsedUsernameReturnsConflictAndUsernameAlreadyInUseAndDoesNotSaveUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: username_already_in_use"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UsernameAlreadyInUseException));

        assertAll(
                () -> assertFalse(userRepository.existsByEmail(email)),
                () -> assertNull(userRepository.findByEmail(email))
        );
    }

    private static Stream<Arguments> callWithUsedEmailReturnsConflictAndEmailAlreadyInUseAndDoesNotSaveUser() {
        return Stream.of(
            arguments("username2", "username@email.com", "password"),
            arguments("username2", "username@email.com", "password2"),
            arguments("username3", "username@email.com", "password2"),
            arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithUsedEmailReturnsConflictAndEmailAlreadyInUseAndDoesNotSaveUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: email_already_in_use"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof EmailAlreadyInUseException));

        assertAll(
                () -> assertFalse(userRepository.existsByUsername(username))
        );
    }
}
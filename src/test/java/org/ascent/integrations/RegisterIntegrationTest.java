package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class RegisterIntegrationTest extends ContainerEnvironment {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername("username");
        user.setEmail("username@email.com");
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        user.setDisabled(false);
        user.setRole(Role.USER);
        user.setCreatedOn(Instant.now());
        user.setLastLogin(null);

        userRepository.save(user);
        userRepository.flush();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsCreatedAndSuccessAndSavesUser() {
        return Stream.of(
                arguments("username2", "username2@email.com", "password2"),
                arguments("username3", "username3@email.com", "password3"),
                arguments("username4", "username4@email.com", "password4"),
                arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsCreatedAndSuccessAndSavesUser(String username, String email, String password) throws Exception {
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
                        .header("HX-Request", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: success"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Success!</span>")));

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

    private static Stream<Arguments> callWithExistingUsernameReturnsConflictAndUsernameAlreadyInUseAndDoesNotSaveUser() {
        return Stream.of(
                arguments("username", "username2@email.com", "password"),
                arguments("username", "username2@email.com", "password2"),
                arguments("username", "username3@email.com", "password2"),
                arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUsernameReturnsConflictAndUsernameAlreadyInUseAndDoesNotSaveUser(String username, String email, String password) throws Exception {
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
                        .header("HX-Request", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: username_already_in_use"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UsernameAlreadyInUseException))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Username is already in use!</span>")));

        assertAll(
                () -> assertFalse(userRepository.existsByEmail(email)),
                () -> assertNull(userRepository.findByEmail(email))
        );
    }

    private static Stream<Arguments> callWithExistingEmailReturnsConflictAndEmailAlreadyInUseAndDoesNotSaveUser() {
        return Stream.of(
                arguments("username2", "username@email.com", "password"),
                arguments("username2", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingEmailReturnsConflictAndEmailAlreadyInUseAndDoesNotSaveUser(String username, String email, String password) throws Exception {
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
                        .header("HX-Request", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: email_already_in_use"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof EmailAlreadyInUseException))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Email is already in use!</span>")));

        assertFalse(userRepository.existsByUsername(username));
    }
}
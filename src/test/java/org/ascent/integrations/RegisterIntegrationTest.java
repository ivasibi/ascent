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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
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

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + serverPort).build();

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

    private static Stream<Arguments> callWithNonExistingUserReturnsCreatedAndSuccess() {
        return Stream.of(
                arguments("username2", "username2@email.com", "password2"),
                arguments("username3", "username3@email.com", "password3"),
                arguments("username4", "username4@email.com", "password4"),
                arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsCreatedAndSuccess(String username, String email, String password) throws Exception {
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
    }

    private static Stream<Arguments> callWithExistingUsernameReturnsConflictAndUsernameAlreadyInUse() {
        return Stream.of(
                arguments("username", "username2@email.com", "password"),
                arguments("username", "username2@email.com", "password2"),
                arguments("username", "username3@email.com", "password2"),
                arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUsernameReturnsConflictAndUsernameAlreadyInUse(String username, String email, String password) throws Exception {
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
    }

    private static Stream<Arguments> callWithExistingEmailReturnsConflictAndEmailAlreadyInUse() {
        return Stream.of(
                arguments("username2", "username@email.com", "password"),
                arguments("username2", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingEmailReturnsConflictAndEmailAlreadyInUse(String username, String email, String password) throws Exception {
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
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsView() {
        return Stream.of(
                arguments("username2", "username2@email.com", "password2"),
                arguments("username3", "username3@email.com", "password3"),
                arguments("username4", "username4@email.com", "password4"),
                arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsView(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(201, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Success!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithExistingUsernameReturnsView() {
        return Stream.of(
                arguments("username", "username2@email.com", "password"),
                arguments("username", "username2@email.com", "password2"),
                arguments("username", "username3@email.com", "password2"),
                arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUsernameReturnsView(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(409, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Username is already in use!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithExistingEmailReturnsView() {
        return Stream.of(
                arguments("username2", "username@email.com", "password"),
                arguments("username2", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingEmailReturnsView(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(409, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Email is already in use!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithNonExistingUserSavesUser() {
        return Stream.of(
                arguments("username2", "username2@email.com", "password2"),
                arguments("username3", "username3@email.com", "password3"),
                arguments("username4", "username4@email.com", "password4"),
                arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserSavesUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

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

    private static Stream<Arguments> callWithExistingUsernameDoesNotSaveUser() {
        return Stream.of(
                arguments("username", "username2@email.com", "password"),
                arguments("username", "username2@email.com", "password2"),
                arguments("username", "username3@email.com", "password2"),
                arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUsernameDoesNotSaveUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

        assertAll(
                () -> assertFalse(userRepository.existsByEmail(email)),
                () -> assertNull(userRepository.findByEmail(email))
        );
    }

    private static Stream<Arguments> callWithExistingEmailDoesNotSaveUser() {
        return Stream.of(
                arguments("username2", "username@email.com", "password"),
                arguments("username2", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password2"),
                arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingEmailDoesNotSaveUser(String username, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        webTestClient.post()
                .uri("/register")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(registerRequestJson)
                .exchange();

        assertFalse(userRepository.existsByUsername(username));
    }
}
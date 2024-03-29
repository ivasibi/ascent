package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class LoginIntegrationTest extends ContainerEnvironment {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    private LettuceConnectionFactory lettuceConnectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

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
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(null);

        User user2 = new User();
        user2.setUsername("username2");
        user2.setEmail("username2@email.com");
        user2.setPassword(bCryptPasswordEncoder.encode("password2"));
        user2.setDisabled(false);
        user2.setRole(Role.ADMIN);
        user2.setCreatedOn(Instant.now().minus(2, ChronoUnit.HOURS));
        user2.setLastLogin(Instant.now().minus(1, ChronoUnit.HOURS));

        User user3 = new User();
        user3.setUsername("username3");
        user3.setEmail("username3@email.com");
        user3.setPassword(bCryptPasswordEncoder.encode("password3"));
        user3.setDisabled(true);
        user3.setRole(Role.USER);
        user3.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user3.setLastLogin(null);

        userRepository.save(user);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.flush();

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisContainer.getHost());
        redisStandaloneConfiguration.setPort(redisContainer.getMappedPort(6379));
        redisStandaloneConfiguration.setPassword(redisPassword);

        lettuceConnectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration);

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();

        lettuceConnectionFactory.start();
    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();

        Set<String> redisKeys = redisTemplate.keys("*");
        if (redisKeys != null) {
            redisTemplate.delete(redisKeys);
        }

        lettuceConnectionFactory.stop();
    }

    private static Stream<Arguments> callWithExistingUserReturnsOkAndSuccess() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUserReturnsOkAndSuccess(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/login_response :: success"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Success!</span>")));
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsUnauthorizedAndInvalidCredentials() {
        return Stream.of(
                arguments("username4@email.com", "password4"),
                arguments("username5@email.com", "password5"),
                arguments("username6@email.com", "password6")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsUnauthorizedAndInvalidCredentials(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/login_response :: invalid_credentials"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidCredentialsException))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Invalid credentials!</span>")));
    }

    private static Stream<Arguments> callWithDisabledUserReturnsUnauthorizedAndDisabledUser() {
        return Stream.of(
                arguments("username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithDisabledUserReturnsUnauthorizedAndDisabledUser(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/login_response :: user_disabled"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UserDisabledException))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">User disabled!</span>")));
    }

    private static Stream<Arguments> callWithNonMatchingPasswordReturnsUnauthorizedAndInvalidCredentials() {
        return Stream.of(
                arguments("username@email.com", "password2"),
                arguments("username2@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonMatchingPasswordReturnsUnauthorizedAndInvalidCredentials(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/login_response :: invalid_credentials"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidCredentialsException))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1\">Invalid credentials!</span>")));
    }

    private static Stream<Arguments> callWithExistingUserReturnsView() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUserReturnsView(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(String.class).getResponseCookies();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(200, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseCookies);
                    assertEquals(1, responseCookies.size());
                    assertTrue(responseCookies.containsKey("SESSION"));
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Success!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsView() {
        return Stream.of(
                arguments("username4@email.com", "password4"),
                arguments("username5@email.com", "password5"),
                arguments("username6@email.com", "password6")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsView(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(String.class).getResponseCookies();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(401, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseCookies);
                    assertEquals(0, responseCookies.size());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Invalid credentials!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithDisabledUserReturnsView() {
        return Stream.of(
                arguments("username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithDisabledUserReturnsView(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(String.class).getResponseCookies();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(401, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseCookies);
                    assertEquals(0, responseCookies.size());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">User disabled!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithNonMatchingPasswordReturnsView() {
        return Stream.of(
                arguments("username@email.com", "password2"),
                arguments("username2@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonMatchingPasswordReturnsView(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(String.class).getResponseCookies();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(401, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseCookies);
                    assertEquals(0, responseCookies.size());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Invalid credentials!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithExistingUserReturnsSessionAndUpdatesUser() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUserReturnsSessionAndUpdatesUser(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        Instant lastLogin = userRepository.findByEmail(email).getLastLogin();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().exists("SESSION");

        Set<String> redisKeys = redisTemplate.keys("*");

        assumeTrue(redisKeys != null);
        assumeTrue(redisKeys.size() == 1);

        String sessionKey = redisKeys.toArray()[0].toString();

        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User user = userRepository.findByEmail(email);
                    assertAll(
                            () -> assertEquals(6, redisTemplate.opsForHash().size(sessionKey)),
                            () -> {
                                Object sessionLogged = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:logged");
                                assertNotNull(sessionLogged);
                                assertAll(
                                        () -> assertTrue(sessionLogged instanceof Boolean),
                                        () -> assertTrue((boolean) sessionLogged)
                                );
                            },
                            () -> {
                                Object sessionUsername = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:username");
                                assertNotNull(sessionUsername);
                                assertAll(
                                        () -> assertTrue(sessionUsername instanceof String),
                                        () -> assertEquals(sessionUsername, user.getUsername())
                                );
                            },
                            () -> {
                                Object sessionRole = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:role");
                                assertNotNull(sessionRole);
                                assertAll(
                                        () -> assertTrue(sessionRole instanceof Role),
                                        () -> assertEquals(sessionRole, user.getRole())
                                );
                            },
                            () -> assertNotEquals(lastLogin, user.getLastLogin()),
                            () -> {
                                if (lastLogin != null) {
                                    assertTrue(lastLogin.isBefore(user.getLastLogin()));
                                }
                            }
                    );
                }
        );
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsNoSession() {
        return Stream.of(
                arguments("username4@email.com", "password4"),
                arguments("username5@email.com", "password5"),
                arguments("username6@email.com", "password6")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserReturnsNoSession(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist("SESSION");

        Set<String> redisKeys = redisTemplate.keys("*");

        assumeTrue(redisKeys != null);
        assertAll(
                () -> assertFalse(userRepository.existsByEmail(email)),
                () -> assertNull(userRepository.findByEmail(email)),
                () -> assertTrue(redisKeys.isEmpty())
        );
    }

    private static Stream<Arguments> callWithDisabledUserReturnsNoSessionAndDoesNotUpdateUser() {
        return Stream.of(
                arguments("username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithDisabledUserReturnsNoSessionAndDoesNotUpdateUser(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        Instant lastLogin = userRepository.findByEmail(email).getLastLogin();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist("SESSION");

        Set<String> redisKeys = redisTemplate.keys("*");

        assumeTrue(redisKeys != null);
        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User user = userRepository.findByEmail(email);
                    assertAll(
                            () -> assertTrue(redisKeys.isEmpty()),
                            () -> assertEquals(lastLogin, user.getLastLogin())
                    );
                }
        );
    }

    private static Stream<Arguments> callWithNonMatchingPasswordReturnsNoSessionAndDoesNotUpdateUser() {
        return Stream.of(
                arguments("username@email.com", "password2"),
                arguments("username2@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonMatchingPasswordReturnsNoSessionAndDoesNotUpdateUser(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        Instant lastLogin = userRepository.findByEmail(email).getLastLogin();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        webTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist("SESSION");

        Set<String> redisKeys = redisTemplate.keys("*");

        assumeTrue(redisKeys != null);
        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User user = userRepository.findByEmail(email);
                    assertAll(
                            () -> assertTrue(redisKeys.isEmpty()),
                            () -> assertEquals(lastLogin, user.getLastLogin())
                    );
                }
        );
    }
}
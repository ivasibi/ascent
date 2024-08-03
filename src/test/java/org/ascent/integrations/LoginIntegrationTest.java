package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
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
import java.util.concurrent.TimeUnit;
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

    private WebTestClient serverTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;

    private EntityTransaction entityTransaction;

    private LettuceConnectionFactory lettuceConnectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        serverTestClient = WebTestClient.bindToServer().baseUrl(serverProtocol + serverIP + ":" + serverPort).build();

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
        user2.setRole(Role.EDITOR);
        user2.setCreatedOn(Instant.now().minus(2, ChronoUnit.HOURS));
        user2.setLastLogin(Instant.now().minus(1, ChronoUnit.HOURS));

        User user3 = new User();
        user3.setUsername("username3");
        user3.setEmail("username3@email.com");
        user3.setPassword(bCryptPasswordEncoder.encode("password3"));
        user3.setDisabled(false);
        user3.setRole(Role.MODERATOR);
        user3.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user3.setLastLogin(null);

        User user4 = new User();
        user4.setUsername("username4");
        user4.setEmail("username4@email.com");
        user4.setPassword(bCryptPasswordEncoder.encode("password4"));
        user4.setDisabled(false);
        user4.setRole(Role.ADMIN);
        user4.setCreatedOn(Instant.now().minus(2, ChronoUnit.HOURS));
        user4.setLastLogin(Instant.now().minus(1, ChronoUnit.HOURS));

        User user5 = new User();
        user5.setUsername("username5");
        user5.setEmail("username5@email.com");
        user5.setPassword(bCryptPasswordEncoder.encode("password5"));
        user5.setDisabled(true);
        user5.setRole(Role.USER);
        user5.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user5.setLastLogin(null);

        userRepository.save(user);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        userRepository.save(user5);
        userRepository.flush();

        entityManager = entityManagerFactory.createEntityManager();
        entityTransaction = entityManager.getTransaction();

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
        entityTransaction.begin();

        entityManager.createQuery("DELETE FROM User").executeUpdate();

        entityTransaction.commit();
        entityManager.close();

        Set<String> redisKeys = redisTemplate.keys("*");
        if (redisKeys != null) {
            redisTemplate.delete(redisKeys);
        }

        lettuceConnectionFactory.stop();
    }

    private static Stream<Arguments> callWithExistingUserReturnsOkAndSuccess() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2"),
                arguments("username3@email.com", "password3"),
                arguments("username4@email.com", "password4")
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
                arguments("username6@email.com", "password6"),
                arguments("username7@email.com", "password7"),
                arguments("username8@email.com", "password8")
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
                arguments("username5@email.com", "password5")
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
                arguments("username2@email.com", "password3"),
                arguments("username3@email.com", "password4"),
                arguments("username4@email.com", "password5")
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
                arguments("username2@email.com", "password2"),
                arguments("username3@email.com", "password3"),
                arguments("username4@email.com", "password4")
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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
                    assertTrue(responseCookies.containsKey(sessionCookieName));
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1\">Success!</span>"));
                }
        );
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsView() {
        return Stream.of(
                arguments("username6@email.com", "password6"),
                arguments("username7@email.com", "password7"),
                arguments("username8@email.com", "password8")
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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
                arguments("username5@email.com", "password5")
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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
                arguments("username2@email.com", "password3"),
                arguments("username3@email.com", "password4"),
                arguments("username4@email.com", "password5")
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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
                arguments("username2@email.com", "password2"),
                arguments("username3@email.com", "password3"),
                arguments("username4@email.com", "password4")
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

        serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().exists(sessionCookieName);

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        assumeTrue(redisSessionKeys != null);
        assumeTrue(redisSessionKeys.size() == 1);

        String sessionKey = redisSessionKeys.toArray()[0].toString();

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> assumeTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    assertAll(
                            () -> {
                                Object sessionUsername = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:username");
                                assertNotNull(sessionUsername);
                                assertAll(
                                        () -> assertTrue(sessionUsername instanceof String),
                                        () -> assertEquals(sessionUsername, persistenceUser.getUsername())
                                );
                            },
                            () -> {
                                Object sessionRole = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:role");
                                assertNotNull(sessionRole);
                                assertAll(
                                        () -> assertTrue(sessionRole instanceof Role),
                                        () -> assertEquals(sessionRole, persistenceUser.getRole())
                                );
                            },
                            () -> assertNotEquals(lastLogin, persistenceUser.getLastLogin()),
                            () -> {
                                if (lastLogin != null) {
                                    assertTrue(lastLogin.isBefore(persistenceUser.getLastLogin()));
                                } else {
                                    assertNotNull(persistenceUser.getLastLogin());
                                }
                            }
                    );
                },
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
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> {
                                Object sessionUsername = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:username");
                                assertNotNull(sessionUsername);
                                assertAll(
                                        () -> assertTrue(sessionUsername instanceof String),
                                        () -> assertEquals(sessionUsername, cacheUser.getUsername())
                                );
                            },
                            () -> {
                                Object sessionRole = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:role");
                                assertNotNull(sessionRole);
                                assertAll(
                                        () -> assertTrue(sessionRole instanceof Role),
                                        () -> assertEquals(sessionRole, cacheUser.getRole())
                                );
                            },
                            () -> assertNotEquals(lastLogin, cacheUser.getLastLogin()),
                            () -> {
                                if (lastLogin != null) {
                                    assertTrue(lastLogin.isBefore(cacheUser.getLastLogin()));
                                } else {
                                    assertNotNull(cacheUser.getLastLogin());
                                }
                            }
                    );
                }
        );
    }

    private static Stream<Arguments> callWithNonExistingUserReturnsNoSession() {
        return Stream.of(
                arguments("username6@email.com", "password6"),
                arguments("username7@email.com", "password7"),
                arguments("username8@email.com", "password8")
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

        serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist(sessionCookieName);

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assumeTrue(redisSessionKeys != null);
        assertAll(
                () -> assertFalse(userRepository.existsByEmail(email)),
                () -> assertNull(userRepository.findByEmail(email)),
                () -> assertThrows(NoResultException.class,
                        () -> query.getSingleResult()),
                () -> assertTrue(redisSessionKeys.isEmpty()),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNull(cacheUser);
                }
        );
    }

    private static Stream<Arguments> callWithDisabledUserReturnsNoSessionAndDoesNotUpdateUser() {
        return Stream.of(
                arguments("username5@email.com", "password5")
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

        serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist(sessionCookieName);

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assumeTrue(redisSessionKeys != null);
        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    if (lastLogin != null) {
                        Instant persistenceUserLastLogin = persistenceUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                        Instant truncatedLastLogin = lastLogin.truncatedTo(ChronoUnit.SECONDS);
                        assertEquals(0, truncatedLastLogin.compareTo(persistenceUserLastLogin));
                    } else {
                        assertNull(persistenceUser.getLastLogin());
                    }
                },
                () -> assertTrue(redisSessionKeys.isEmpty()),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    if (lastLogin != null) {
                        Instant cacheUserLastLogin = cacheUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                        Instant truncatedLastLogin = lastLogin.truncatedTo(ChronoUnit.SECONDS);
                        assertEquals(0, truncatedLastLogin.compareTo(cacheUserLastLogin));
                    } else {
                        assertNull(cacheUser.getLastLogin());
                    }
                }
        );
    }

    private static Stream<Arguments> callWithNonMatchingPasswordReturnsNoSessionAndDoesNotUpdateUser() {
        return Stream.of(
                arguments("username@email.com", "password2"),
                arguments("username2@email.com", "password3"),
                arguments("username3@email.com", "password4"),
                arguments("username4@email.com", "password5")
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

        serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().doesNotExist(sessionCookieName);

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assumeTrue(redisSessionKeys != null);
        assertAll(
                () -> assertTrue(userRepository.existsByEmail(email)),
                () -> assertNotNull(userRepository.findByEmail(email)),
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    if (lastLogin != null) {
                        Instant persistenceUserLastLogin = persistenceUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                        Instant truncatedLastLogin = lastLogin.truncatedTo(ChronoUnit.SECONDS);
                        assertEquals(0, truncatedLastLogin.compareTo(persistenceUserLastLogin));
                    } else {
                        assertNull(persistenceUser.getLastLogin());
                    }
                },
                () -> assertTrue(redisSessionKeys.isEmpty()),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    if (lastLogin != null) {
                        Instant cacheUserLastLogin = cacheUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                        Instant truncatedLastLogin = lastLogin.truncatedTo(ChronoUnit.SECONDS);
                        assertEquals(0, truncatedLastLogin.compareTo(cacheUserLastLogin));
                    } else {
                        assertNull(cacheUser.getLastLogin());
                    }
                }
        );
    }

    private static Stream<Arguments> callWithExistingUserReturnsSessionWithInactiveInterval() {
        return Stream.of(
            arguments("username@email.com", "password", 120 * 60),
            arguments("username2@email.com", "password2", 60 * 60),
            arguments("username3@email.com", "password3", 30 * 60),
            arguments("username4@email.com", "password4", 15 * 60)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUserReturnsSessionWithInactiveInterval(String email, String password, int maxInactiveInterval) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().exists(sessionCookieName);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        assumeTrue(redisSessionKeys != null);
        assumeTrue(redisSessionKeys.size() == 1);

        String sessionKey = redisSessionKeys.toArray()[0].toString();

        Object sessionMaxInactiveInterval = redisTemplate.opsForHash().get(sessionKey, "maxInactiveInterval");

        assertNotNull(sessionMaxInactiveInterval);

        assertAll(
                () -> assertEquals(maxInactiveInterval, sessionMaxInactiveInterval),
                () -> {
                    Long sessionKeyTTL = redisTemplate.getExpire(sessionKey);
                    assertNotNull(sessionKeyTTL);
                    assertAll(
                            () -> assertTrue(sessionKeyTTL.intValue() >= maxInactiveInterval - 10),
                            () -> assertTrue(sessionKeyTTL.intValue() <= maxInactiveInterval)
                    );
                }
        );
    }

    private static Stream<Arguments> callAnywhereWithSessionRestoresInactiveInterval() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2"),
                arguments("username3@email.com", "password3"),
                arguments("username4@email.com", "password4")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callAnywhereWithSessionRestoresInactiveInterval(String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().exists(sessionCookieName);

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        assumeTrue(redisSessionKeys != null);
        assumeTrue(redisSessionKeys.size() == 1);

        String sessionKey = redisSessionKeys.toArray()[0].toString();

        Object sessionMaxInactiveInterval = redisTemplate.opsForHash().get(sessionKey, "maxInactiveInterval");

        assertNotNull(sessionMaxInactiveInterval);

        assertAll(
                () -> {
                    Long sessionKeyTTL = redisTemplate.getExpire(sessionKey);
                    assertNotNull(sessionKeyTTL);
                    assertAll(
                            () -> assertTrue(sessionKeyTTL.intValue() >= (Integer) sessionMaxInactiveInterval - 10),
                            () -> assertTrue(sessionKeyTTL.intValue() <= (Integer) sessionMaxInactiveInterval)
                    );
                }
        );

        redisTemplate.expire(sessionKey, 10, TimeUnit.SECONDS);

        assertAll(
                () -> {
                    Long sessionKeyTTL = redisTemplate.getExpire(sessionKey);
                    assertNotNull(sessionKeyTTL);
                    assertAll(
                            () -> assertTrue(sessionKeyTTL.intValue() >= 0),
                            () -> assertTrue(sessionKeyTTL.intValue() <= 10)
                    );
                }
        );

        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(Void.class).getResponseCookies();

        assumeTrue(responseCookies.size() == 1);

        String sessionCookie = responseCookies.get(sessionCookieName).get(0).getValue();

        serverTestClient.get()
                .uri("/")
                    .cookie(sessionCookieName, sessionCookie)
                .exchange();

        assertAll(
                () -> {
                    Long sessionKeyTTL = redisTemplate.getExpire(sessionKey);
                    assertNotNull(sessionKeyTTL);
                    assertAll(
                            () -> assertTrue(sessionKeyTTL.intValue() >= (Integer) sessionMaxInactiveInterval - 10),
                            () -> assertTrue(sessionKeyTTL.intValue() <= (Integer) sessionMaxInactiveInterval)
                    );
                }
        );
    }
}
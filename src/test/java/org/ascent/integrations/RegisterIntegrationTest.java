package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.filters.LoggingFilter;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class RegisterIntegrationTest extends ContainerEnvironment {

    private final static Logger logger = LoggerFactory.getLogger(RegisterIntegrationTest.class.getName());

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilters(new LoggingFilter()).build();

        serverTestClient = WebTestClient.bindToServer().baseUrl(serverProtocol + serverHostIP + ":" + serverHostPort).build();

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
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();

        lettuceConnectionFactory.start();

        File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);
        try {
            Files.writeString(loggingFile.toPath(), "");
        } catch (IOException e) {
            logger.error("{} {}", "IOException", e.getMessage());
        }
    }

    @AfterEach
    public void afterEach() {
        entityTransaction.begin();

        entityManager.createQuery("DELETE FROM User").executeUpdate();

        entityTransaction.commit();
        entityManager.close();

        Set<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);

        lettuceConnectionFactory.stop();
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
            .andExpect(result -> assertInstanceOf(UsernameAlreadyInUseException.class, result.getResolvedException()))
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
            .andExpect(result -> assertInstanceOf(EmailAlreadyInUseException.class, result.getResolvedException()))
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
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
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
            () -> assertTrue(userRepository.existsByUsername(username)),
            () -> assertTrue(userRepository.existsByEmail(email)),
            () -> assertNotNull(userRepository.findByEmail(email)),
            () -> {
                User persistenceUser = query.getSingleResult();
                assertNotNull(persistenceUser);
                assertAll(
                    () -> assertNotNull(persistenceUser.getId()),
                    () -> assertEquals(username, persistenceUser.getUsername()),
                    () -> assertEquals(email, persistenceUser.getEmail()),
                    () -> {
                        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                        assertTrue(bCryptPasswordEncoder.matches(password, persistenceUser.getPassword()));
                    },
                    () -> assertFalse(persistenceUser.isDisabled()),
                    () -> assertEquals(Role.USER, persistenceUser.getRole()),
                    () -> assertNotNull(persistenceUser.getCreatedOn()),
                    () -> assertNull(persistenceUser.getLastLogin())
                );
            },
            () -> {
                Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                User cacheUser = (User) cacheObject;
                assertNotNull(cacheUser);
                assertAll(
                    () -> assertNotNull(cacheUser.getId()),
                    () -> assertEquals(username, cacheUser.getUsername()),
                    () -> assertEquals(email, cacheUser.getEmail()),
                    () -> {
                        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                        assertTrue(bCryptPasswordEncoder.matches(password, cacheUser.getPassword()));
                    },
                    () -> assertFalse(cacheUser.isDisabled()),
                    () -> assertEquals(Role.USER, cacheUser.getRole()),
                    () -> assertNotNull(cacheUser.getCreatedOn()),
                    () -> assertNull(cacheUser.getLastLogin())
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
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
            () -> assertFalse(userRepository.existsByEmail(email)),
            () -> assertNull(userRepository.findByEmail(email)),
            () -> assertThrows(NoResultException.class,
                () -> query.getSingleResult()),
            () -> {
                Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                User cacheUser = (User) cacheObject;
                assertNull(cacheUser);
            }
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

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        assertFalse(userRepository.existsByUsername(username));
    }

    private static Stream<Arguments> callWithNonExistingUserLogsRequestOnFile() {
        return Stream.of(
            arguments("username2", "username2@email.com", "password2"),
            arguments("username3", "username3@email.com", "password3"),
            arguments("username4", "username4@email.com", "password4"),
            arguments("username5", "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithNonExistingUserLogsRequestOnFile(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        assertAll(
            () -> {
                File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);
                assertTrue(loggingFile.exists());
                String loggingFileContent = new String(Files.readAllBytes(loggingFile.toPath()));
                assertAll(
                    () -> assertTrue(loggingFileContent.contains("INFO")),
                    () -> assertTrue(loggingFileContent.contains("ascent.filters.LoggingFilter")),
                    () -> assertTrue(loggingFileContent.contains("127.0.0.1")),
                    () -> assertTrue(loggingFileContent.contains("POST")),
                    () -> assertTrue(loggingFileContent.contains("/register")),
                    () -> assertTrue(loggingFileContent.contains("DEBUG")),
                    () -> assertTrue(loggingFileContent.contains("ascent.controllers.RegisterController")),
                    () -> {
                        StringBuilder registerRequestString = new StringBuilder();
                        registerRequestString.append("RegisterRequest(username=");
                        registerRequestString.append(username);
                        registerRequestString.append(", email=");
                        registerRequestString.append(email);
                        registerRequestString.append(")");
                        assertTrue(loggingFileContent.contains(registerRequestString));
                    },
                    () -> assertTrue(loggingFileContent.contains("201"))
                );
            }
        );
    }

    private static Stream<Arguments> callWithExistingUsernameLogsWarningOnFile() {
        return Stream.of(
            arguments("username", "username2@email.com", "password"),
            arguments("username", "username2@email.com", "password2"),
            arguments("username", "username3@email.com", "password2"),
            arguments("username", "username3@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingUsernameLogsWarningOnFile(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        assertAll(
            () -> {
                File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);
                assertTrue(loggingFile.exists());
                String loggingFileContent = new String(Files.readAllBytes(loggingFile.toPath()));
                assertAll(
                    () -> assertTrue(loggingFileContent.contains("INFO")),
                    () -> assertTrue(loggingFileContent.contains("ascent.filters.LoggingFilter")),
                    () -> assertTrue(loggingFileContent.contains("127.0.0.1")),
                    () -> assertTrue(loggingFileContent.contains("POST")),
                    () -> assertTrue(loggingFileContent.contains("/register")),
                    () -> assertTrue(loggingFileContent.contains("DEBUG")),
                    () -> assertTrue(loggingFileContent.contains("WARN")),
                    () -> assertTrue(loggingFileContent.contains("ascent.controllers.RegisterController")),
                    () -> {
                        StringBuilder registerRequestString = new StringBuilder();
                        registerRequestString.append("RegisterRequest(username=");
                        registerRequestString.append(username);
                        registerRequestString.append(", email=");
                        registerRequestString.append(email);
                        registerRequestString.append(")");
                        assertTrue(loggingFileContent.contains(registerRequestString));
                    },
                    () -> assertTrue(loggingFileContent.contains("UsernameAlreadyInUseException")),
                    () -> assertTrue(loggingFileContent.contains("409"))
                );
            }
        );
    }

    private static Stream<Arguments> callWithExistingEmailLogsWarningOnFile() {
        return Stream.of(
            arguments("username2", "username@email.com", "password"),
            arguments("username2", "username@email.com", "password2"),
            arguments("username3", "username@email.com", "password2"),
            arguments("username3", "username@email.com", "password3")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callWithExistingEmailLogsWarningOnFile(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        serverTestClient.post()
            .uri("/register")
                .header("HX-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerRequestJson)
            .exchange();

        assertAll(
            () -> {
                File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);
                assertTrue(loggingFile.exists());
                String loggingFileContent = new String(Files.readAllBytes(loggingFile.toPath()));
                assertAll(
                    () -> assertTrue(loggingFileContent.contains("INFO")),
                    () -> assertTrue(loggingFileContent.contains("ascent.filters.LoggingFilter")),
                    () -> assertTrue(loggingFileContent.contains("127.0.0.1")),
                    () -> assertTrue(loggingFileContent.contains("POST")),
                    () -> assertTrue(loggingFileContent.contains("/register")),
                    () -> assertTrue(loggingFileContent.contains("DEBUG")),
                    () -> assertTrue(loggingFileContent.contains("WARN")),
                    () -> assertTrue(loggingFileContent.contains("ascent.controllers.RegisterController")),
                    () -> {
                        StringBuilder registerRequestString = new StringBuilder();
                        registerRequestString.append("RegisterRequest(username=");
                        registerRequestString.append(username);
                        registerRequestString.append(", email=");
                        registerRequestString.append(email);
                        registerRequestString.append(")");
                        assertTrue(loggingFileContent.contains(registerRequestString));
                    },
                    () -> assertTrue(loggingFileContent.contains("EmailAlreadyInUseException")),
                    () -> assertTrue(loggingFileContent.contains("409"))
                );
            }
        );
    }
}
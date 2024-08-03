package org.ascent.functionalities;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.ascent.requests.RegisterRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class LoginFunctionalityTest extends ContainerEnvironment {

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
        serverTestClient = WebTestClient.bindToServer().baseUrl(serverProtocol + serverIP + ":" + serverPort).build();

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

    private static Stream<Arguments> registerNewUserThenLogin() {
        return Stream.of(
                arguments("username", "username@email.com", "password", 120 * 60),
                arguments("username2", "username2@email.com", "password2", 120 * 60)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void registerNewUserThenLogin(String username, String email, String password, int maxInactiveInterval) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        ObjectMapper objectMapper = new ObjectMapper();

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

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

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = serverTestClient.post()
                .uri("/login")
                    .header("HX-Request", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(loginRequestJson)
                .exchange()
                .expectCookie().exists(sessionCookieName);

        entityManager.clear();

        Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");

        assumeTrue(redisSessionKeys != null);
        assumeTrue(redisSessionKeys.size() == 1);

        String sessionKey = redisSessionKeys.toArray()[0].toString();

        assertAll(
                () -> assertTrue(userRepository.existsByUsername(username)),
                () -> assertTrue(userRepository.existsByEmail(email)),
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
                            () -> assertNotNull(persistenceUser.getLastLogin()),
                            () -> assertTrue(persistenceUser.getCreatedOn().isBefore(persistenceUser.getLastLogin()))
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
                            () -> assertNotNull(cacheUser.getLastLogin()),
                            () -> assertTrue(cacheUser.getCreatedOn().isBefore(cacheUser.getLastLogin()))
                    );
                }
        );
    }
}
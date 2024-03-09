package org.ascent.functionalities;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    private LettuceConnectionFactory lettuceConnectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    public void beforeEach() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + serverPort).build();

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

    private static Stream<Arguments> registerNewUserThenLogin() {
        return Stream.of(
                arguments("username", "username@email.com", "password"),
                arguments("username2", "username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void registerNewUserThenLogin(String username, String email, String password) throws Exception {
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

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);

        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        WebTestClient.ResponseSpec responseSpec = webTestClient.post()
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
                () -> assertTrue(userRepository.existsByUsername(username)),
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
                            () -> assertNotNull(user.getLastLogin()),
                            () -> assertTrue(user.getCreatedOn().isBefore(user.getLastLogin()))
                    );
                }
        );
    }
}
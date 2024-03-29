package org.ascent.integrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.mock.web.MockHttpSession;
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

public class ViewIntegrationTest extends ContainerEnvironment {

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

        userRepository.save(user);
        userRepository.save(user2);
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

    private static Stream<Role> callIndexWithLoggedUserReturnsOkAndIndex() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void callIndexWithLoggedUserReturnsOkAndIndex(Role role) throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", true);
        mockHttpSession.setAttribute("username", "username");
        mockHttpSession.setAttribute("role", role);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(3))
                .andExpect(model().attribute("logged", true))
                .andExpect(model().attribute("username", "username"))
                .andExpect(model().attribute("role", role))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>")));
    }

    @Test
    public void callIndexWithoutLoggedUserReturnsOkAndIndex() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", false);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    @Test
    public void callIndexWithoutSessionReturnsOkAndIndex() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", null);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    private static Stream<Arguments> callIndexWithLoggedUserReturnsView() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callIndexWithLoggedUserReturnsView(String email, String password) throws Exception {
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

        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(Void.class).getResponseCookies();

        assumeTrue(responseCookies.size() == 1);

        String sessionCookie = responseCookies.get("SESSION").get(0).getValue();

        responseSpec = webTestClient.get()
                .uri("/")
                    .cookie("SESSION", sessionCookie)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(200, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>"));
                }
        );
    }

    @Test
    public void callIndexWithoutLoggedUserReturnsView() {
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/")
                    .cookie("SESSION", "session")
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(200, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>"));
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>"));
                }
        );
    }

    private static Stream<Role> callNavbarWithLoggedUserReturnsOkAndNavbar() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void callNavbarWithLoggedUserReturnsOkAndNavbar(Role role) throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", true);
        mockHttpSession.setAttribute("username", "username");
        mockHttpSession.setAttribute("role", role);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(3))
                .andExpect(model().attribute("logged", true))
                .andExpect(model().attribute("username", "username"))
                .andExpect(model().attribute("role", role))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>")));
    }

    @Test
    public void callNavbarWithoutLoggedUserReturnsOkAndNavbar() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", false);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    @Test
    public void callNavbarWithoutSessionReturnsOkAndNavbar() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", null);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    private static Stream<Arguments> callNavbarWithLoggedUserReturnsView() {
        return Stream.of(
                arguments("username@email.com", "password"),
                arguments("username2@email.com", "password2")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void callNavbarWithLoggedUserReturnsView(String email, String password) throws Exception {
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

        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(Void.class).getResponseCookies();

        assumeTrue(responseCookies.size() == 1);

        String sessionCookie = responseCookies.get("SESSION").get(0).getValue();

        responseSpec = webTestClient.get()
                .uri("/navbar")
                    .cookie("SESSION", sessionCookie)
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(200, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>"));
                }
        );
    }

    @Test
    public void callNavbarWithoutLoggedUserReturnsView() {
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        WebTestClient.ResponseSpec responseSpec = webTestClient.get()
                .uri("/navbar")
                    .cookie("SESSION", "session")
                .exchange();

        HttpStatusCode responseStatusCode = responseSpec.returnResult(String.class).getStatus();
        HttpHeaders responseHeaders = responseSpec.returnResult(String.class).getResponseHeaders();
        String responseBody = responseSpec.expectBody(String.class).returnResult().getResponseBody();

        assertAll(
                () -> assertEquals(200, responseStatusCode.value()),
                () -> {
                    Object contentType = responseHeaders.get("Content-Type");
                    assertNotNull(contentType);
                    assertEquals("[text/html;charset=UTF-8]", contentType.toString());
                },
                () -> {
                    assertNotNull(responseBody);
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>"));
                    assertTrue(responseBody.contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>"));
                }
        );
    }
}
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode;
import org.testcontainers.containers.VncRecordingContainer.VncRecordingFormat;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

@Testcontainers
public class ViewFunctionalityTest extends ContainerEnvironment {

    private final static Logger logger = LoggerFactory.getLogger(ViewFunctionalityTest.class.getName());

    private WebDriver chromeWebDriver;

    @Container
    private BrowserWebDriverContainer<?> chromeContainer = new BrowserWebDriverContainer<>(chromeImage)
        .withCapabilities(new ChromeOptions())
        .withRecordingMode(vncRecordingMode, new File(recordingFilePathPrefix + "/chrome"), VncRecordingFormat.MP4);

    private WebDriver firefoxWebDriver;

    @Container
    private BrowserWebDriverContainer<?> firefoxContainer = new BrowserWebDriverContainer<>(firefoxImage)
        .withCapabilities(new FirefoxOptions())
        .withRecordingMode(vncRecordingMode, new File(recordingFilePathPrefix + "/firefox"), VncRecordingFormat.MP4);

    private WebDriver edgeWebDriver;

    @Container
    private BrowserWebDriverContainer<?> edgeContainer = new BrowserWebDriverContainer<>(edgeImage)
        .withCapabilities(new EdgeOptions())
        .withRecordingMode(vncRecordingMode, new File(recordingFilePathPrefix + "/edge"), VncRecordingFormat.MP4);

    private enum WebBrowser {
        CHROME,
        FIREFOX,
        EDGE
    }

    private Actions actions;

    private WebDriverWait webDriverWait;

    private static Map<String, Color> colors;

    private WebTestClient serverTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private EntityManager entityManager;

    private EntityTransaction entityTransaction;

    private LettuceConnectionFactory lettuceConnectionFactory;

    private RedisTemplate<String, Object> redisTemplate;

    @BeforeAll
    public static void beforeAll() {
        colors = new HashMap<>();

        colors.put("PRIMARY", Color.fromString("#0D6EFD"));
        colors.put("PRIMARY_HOVER", Color.fromString("#0B5ED7"));
        colors.put("PRIMARY_OUTLINE_HOVER", Color.fromString("#0D6EFD"));

        colors.put("SUCCESS", Color.fromString("#198754"));
        colors.put("SUCCESS_HOVER", Color.fromString("#157347"));
        colors.put("SUCCESS_OUTLINE_HOVER", Color.fromString("#198754"));

        colors.put("WARNING", Color.fromString("#FFC107"));

        colors.put("DANGER", Color.fromString("#DC3545"));
        colors.put("DANGER_HOVER", Color.fromString("#BB2D3B"));
        colors.put("DANGER_OUTLINE_HOVER", Color.fromString("#DC3545"));

        colors.put("LIGHT", Color.fromString("#F8F9FA"));

        colors.put("DARK", Color.fromString("#212529"));
        colors.put("DARK_HOVER", Color.fromString("#424649"));
    }

    @BeforeEach
    public void beforeEach() {
        new File(recordingFilePathPrefix + "/chrome").mkdirs();
        new File(recordingFilePathPrefix + "/firefox").mkdirs();
        new File(recordingFilePathPrefix + "/edge").mkdirs();

        chromeWebDriver = new RemoteWebDriver(chromeContainer.getSeleniumAddress(), new ChromeOptions());
        firefoxWebDriver = new RemoteWebDriver(firefoxContainer.getSeleniumAddress(), new FirefoxOptions());
        edgeWebDriver = new RemoteWebDriver(edgeContainer.getSeleniumAddress(), new EdgeOptions());

        serverTestClient = WebTestClient.bindToServer().baseUrl(serverProtocol + serverHostIP + ":" + serverHostPort).build();

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
        user2.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user2.setLastLogin(null);

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
        user4.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user4.setLastLogin(null);

        userRepository.save(user);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
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
        chromeWebDriver.quit();
        firefoxWebDriver.quit();
        edgeWebDriver.quit();

        entityTransaction.begin();

        entityManager.createQuery("DELETE FROM User").executeUpdate();

        entityTransaction.commit();
        entityManager.close();

        Set<String> redisKeys = redisTemplate.keys("*");
        redisTemplate.delete(redisKeys);

        lettuceConnectionFactory.stop();
    }

    private static Stream<Arguments> indexRegisterFunctionality() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username5", "username5@email.com", "password5"),
            arguments(WebBrowser.CHROME, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.CHROME, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username8", "username8@email.com", "password8"),
            arguments(WebBrowser.FIREFOX, "username5", "username5@email.com", "password5"),
            arguments(WebBrowser.FIREFOX, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.FIREFOX, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username8", "username8@email.com", "password8"),
            arguments(WebBrowser.EDGE, "username5", "username5@email.com", "password5"),
            arguments(WebBrowser.EDGE, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.EDGE, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username8", "username8@email.com", "password8")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexRegisterFunctionality(WebBrowser webBrowser, String username, String email, String password) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        WebDriver webDriver;

        switch (webBrowser) {
            case CHROME -> {
                assumeTrue(chromeContainer.isCreated());
                assumeTrue(chromeContainer.isRunning());

                webDriver = chromeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case FIREFOX -> {
                assumeTrue(firefoxContainer.isCreated());
                assumeTrue(firefoxContainer.isRunning());

                webDriver = firefoxWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case EDGE -> {
                assumeTrue(edgeContainer.isCreated());
                assumeTrue(edgeContainer.isRunning());

                webDriver = edgeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            default -> webDriver = null;
        }

        assumeTrue(webDriver != null);

        actions = new Actions(webDriver);
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

        webDriver.manage().window().maximize();
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> {
                WebElement registerButton = webDriver.findElement(By.id("register_button"));
                actions.moveToElement(registerButton).click().perform();

                WebElement registerModal = webDriver.findElement(By.id("register_modal"));
                webDriverWait.until(driver -> registerModal.isDisplayed());
                assertAll(
                    () -> {
                        WebElement registerUsername = registerModal.findElement(By.id("register_username"));
                        registerUsername.sendKeys(username);

                        WebElement registerEmail = registerModal.findElement(By.id("register_email"));
                        registerEmail.sendKeys(email);

                        WebElement registerPassword = registerModal.findElement(By.id("register_password"));
                        registerPassword.sendKeys(password);

                        WebElement registerSubmit = registerModal.findElement(By.id("register_submit"));
                        actions.moveToElement(registerSubmit).click().perform();

                        WebElement registerResponse = registerModal.findElement(By.id("register_response"));
                        webDriverWait.until(driver -> !registerResponse.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement registerSuccessResponse = registerResponse.findElement(By.id("register_success"));
                                assertEquals(colors.get("SUCCESS"), Color.fromString(registerSuccessResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerSuccessResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerSuccessResponseIcon = registerSuccessResponse.findElement(By.tagName("i"));
                                        String registerSuccessResponseIconClass = registerSuccessResponseIcon.getAttribute("class");
                                        assertNotNull(registerSuccessResponseIconClass);
                                        assertTrue(registerSuccessResponseIconClass.contains("fa-solid fa-check"));
                                        assertEquals("Success!", registerSuccessResponse.getText());
                                    }
                                );
                            }
                        );

                        webDriverWait.until(driver -> !registerModal.isDisplayed());

                        assertAll(
                            () -> {
                                String registerUsernameValue = registerUsername.getAttribute("value");
                                assertNotNull(registerUsernameValue);
                                assertTrue(registerUsernameValue.isEmpty());
                            },
                            () -> {
                                String registerEmailValue = registerEmail.getAttribute("value");
                                assertNotNull(registerEmailValue);
                                assertTrue(registerEmailValue.isEmpty());
                            },
                            () -> {
                                String registerPasswordValue = registerPassword.getAttribute("value");
                                assertNotNull(registerPasswordValue);
                                assertTrue(registerPasswordValue.isEmpty());
                            },
                            () -> {
                                String registerResponseInnerHTML = registerResponse.getAttribute("innerHTML");
                                assertNotNull(registerResponseInnerHTML);
                                assertTrue(registerResponseInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            },
            () -> {
                TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
                query.setParameter("email", email);

                String cacheKey = cacheKeyPrefix + ":users:email::" + email;

                File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);

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
                    },
                    () -> assertTrue(loggingFile.exists()),
                    () -> {
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
        );
    }

    private static Stream<Arguments> indexLoginFunctionality() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username", "username@email.com", "password", 120 * 60),
            arguments(WebBrowser.CHROME, "username2", "username2@email.com", "password2", 60 * 60),
            arguments(WebBrowser.CHROME, "username3", "username3@email.com", "password3", 30 * 60),
            arguments(WebBrowser.CHROME, "username4", "username4@email.com", "password4", 15 * 60),
            arguments(WebBrowser.FIREFOX, "username", "username@email.com", "password", 120 * 60),
            arguments(WebBrowser.FIREFOX, "username2", "username2@email.com", "password2", 60 * 60),
            arguments(WebBrowser.FIREFOX, "username3", "username3@email.com", "password3", 30 * 60),
            arguments(WebBrowser.FIREFOX, "username4", "username4@email.com", "password4", 15 * 60),
            arguments(WebBrowser.EDGE, "username", "username@email.com", "password", 120 * 60),
            arguments(WebBrowser.EDGE, "username2", "username2@email.com", "password2", 60 * 60),
            arguments(WebBrowser.EDGE, "username3", "username3@email.com", "password3", 30 * 60),
            arguments(WebBrowser.EDGE, "username4", "username4@email.com", "password4", 15 * 60)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLoginFunctionality(WebBrowser webBrowser, String username, String email, String password, int maxInactiveInterval) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        WebDriver webDriver;

        switch (webBrowser) {
            case CHROME -> {
                assumeTrue(chromeContainer.isCreated());
                assumeTrue(chromeContainer.isRunning());

                webDriver = chromeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case FIREFOX -> {
                assumeTrue(firefoxContainer.isCreated());
                assumeTrue(firefoxContainer.isRunning());

                webDriver = firefoxWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case EDGE -> {
                assumeTrue(edgeContainer.isCreated());
                assumeTrue(edgeContainer.isRunning());

                webDriver = edgeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            default -> webDriver = null;
        }

        assumeTrue(webDriver != null);

        actions = new Actions(webDriver);
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

        webDriver.manage().window().maximize();
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> assertDoesNotThrow(() -> webDriver.findElement(By.id("login_button"))),
            () -> assertDoesNotThrow(() -> webDriver.findElement(By.id("register_button"))),
            () -> {
                WebElement loginButton = webDriver.findElement(By.id("login_button"));
                actions.moveToElement(loginButton).click().perform();

                WebElement loginModal = webDriver.findElement(By.id("login_modal"));
                webDriverWait.until(driver -> loginModal.isDisplayed());
                assertAll(
                    () -> {
                        WebElement loginEmail = loginModal.findElement(By.id("login_email"));
                        loginEmail.sendKeys(email);

                        WebElement loginPassword = loginModal.findElement(By.id("login_password"));
                        loginPassword.sendKeys(password);

                        WebElement loginSubmit = loginModal.findElement(By.id("login_submit"));
                        actions.moveToElement(loginSubmit).click().perform();

                        WebElement loginResponse = loginModal.findElement(By.id("login_response"));
                        webDriverWait.until(driver -> !loginResponse.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement loginSuccessResponse = loginResponse.findElement(By.id("login_success"));
                                assertEquals(colors.get("SUCCESS"), Color.fromString(loginSuccessResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginSuccessResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginSuccessResponseIcon = loginSuccessResponse.findElement(By.tagName("i"));
                                        String loginSuccessResponseIconClass = loginSuccessResponseIcon.getAttribute("class");
                                        assertNotNull(loginSuccessResponseIconClass);
                                        assertTrue(loginSuccessResponseIconClass.contains("fa-solid fa-check"));
                                        assertEquals("Success!", loginSuccessResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(1000);

                        assertAll(
                            () -> {
                                assertThrows(NoSuchElementException.class,
                                    () -> webDriver.findElement(By.id("login_modal")));
                                assertThrows(NoSuchElementException.class,
                                    () -> webDriver.findElement(By.id("login_button")));
                                assertThrows(NoSuchElementException.class,
                                    () -> webDriver.findElement(By.id("register_button")));
                                assertDoesNotThrow(() -> webDriver.findElement(By.id("logout_button")));
                            }
                        );
                    }
                );
            },
            () -> assertNotNull(webDriver.manage().getCookieNamed(sessionCookieName)),
            () -> {
                TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
                query.setParameter("email", email);

                String cacheKey = cacheKeyPrefix + ":users:email::" + email;

                Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");
                assertEquals(1, redisSessionKeys.size());
                String sessionKey = redisSessionKeys.toArray()[0].toString();

                File loggingFile = new File(loggingFilePath + "/" + loggingFileName + loggingFileExtension);

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
                                    () -> assertInstanceOf(String.class, sessionUsername),
                                    () -> assertEquals(sessionUsername, persistenceUser.getUsername())
                                );
                            },
                            () -> {
                                Object sessionRole = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:role");
                                assertNotNull(sessionRole);
                                assertAll(
                                    () -> assertInstanceOf(Role.class, sessionRole),
                                    () -> assertEquals(sessionRole, persistenceUser.getRole())
                                );
                            },
                            () -> assertNotNull(persistenceUser.getLastLogin()),
                            () -> assertTrue(persistenceUser.getCreatedOn().isBefore(persistenceUser.getLastLogin()))
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
                                    () -> assertInstanceOf(String.class, sessionUsername),
                                    () -> assertEquals(sessionUsername, cacheUser.getUsername())
                                );
                            },
                            () -> {
                                Object sessionRole = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:role");
                                assertNotNull(sessionRole);
                                assertAll(
                                    () -> assertInstanceOf(Role.class, sessionRole),
                                    () -> assertEquals(sessionRole, cacheUser.getRole())
                                );
                            },
                            () -> assertNotNull(cacheUser.getLastLogin()),
                            () -> assertTrue(cacheUser.getCreatedOn().isBefore(cacheUser.getLastLogin()))
                        );
                    },
                    () -> assertEquals(6, redisTemplate.opsForHash().size(sessionKey)),
                    () -> {
                        Object sessionLogged = redisTemplate.opsForHash().get(sessionKey, "sessionAttr:logged");
                        assertNotNull(sessionLogged);
                        assertAll(
                            () -> assertInstanceOf(Boolean.class, sessionLogged),
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
                    () -> assertTrue(loggingFile.exists()),
                    () -> {
                        String loggingFileContent = new String(Files.readAllBytes(loggingFile.toPath()));
                        assertAll(
                            () -> assertTrue(loggingFileContent.contains("INFO")),
                            () -> assertTrue(loggingFileContent.contains("ascent.filters.LoggingFilter")),
                            () -> assertTrue(loggingFileContent.contains("127.0.0.1")),
                            () -> assertTrue(loggingFileContent.contains("POST")),
                            () -> assertTrue(loggingFileContent.contains("/login")),
                            () -> assertTrue(loggingFileContent.contains("DEBUG")),
                            () -> assertTrue(loggingFileContent.contains("ascent.controllers.LoginController")),
                            () -> {
                                StringBuilder loginRequestString = new StringBuilder();
                                loginRequestString.append("LoginRequest(email=");
                                loginRequestString.append(email);
                                loginRequestString.append(")");
                                assertTrue(loggingFileContent.contains(loginRequestString));
                            },
                            () -> assertTrue(loggingFileContent.contains("200"))
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLogoutFunctionality() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username@email.com", "password"),
            arguments(WebBrowser.CHROME, "username2@email.com", "password2"),
            arguments(WebBrowser.CHROME, "username3@email.com", "password3"),
            arguments(WebBrowser.CHROME, "username4@email.com", "password4"),
            arguments(WebBrowser.FIREFOX, "username@email.com", "password"),
            arguments(WebBrowser.FIREFOX, "username2@email.com", "password2"),
            arguments(WebBrowser.FIREFOX, "username3@email.com", "password3"),
            arguments(WebBrowser.FIREFOX, "username4@email.com", "password4"),
            arguments(WebBrowser.EDGE, "username@email.com", "password"),
            arguments(WebBrowser.EDGE, "username2@email.com", "password2"),
            arguments(WebBrowser.EDGE, "username3@email.com", "password3"),
            arguments(WebBrowser.EDGE, "username4@email.com", "password4")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLogoutFunctionality(WebBrowser webBrowser, String email, String password) throws Exception {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        WebDriver webDriver;

        switch (webBrowser) {
            case CHROME -> {
                assumeTrue(chromeContainer.isCreated());
                assumeTrue(chromeContainer.isRunning());

                webDriver = chromeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case FIREFOX -> {
                assumeTrue(firefoxContainer.isCreated());
                assumeTrue(firefoxContainer.isRunning());

                webDriver = firefoxWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    edgeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            case EDGE -> {
                assumeTrue(edgeContainer.isCreated());
                assumeTrue(edgeContainer.isRunning());

                webDriver = edgeWebDriver;

                if (vncRecordingMode != VncRecordingMode.SKIP) {
                    chromeContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                    firefoxContainer.withRecordingMode(VncRecordingMode.SKIP, null);
                }
            }
            default -> webDriver = null;
        }

        assumeTrue(webDriver != null);

        actions = new Actions(webDriver);
        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

        webDriver.manage().window().maximize();
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        Thread.sleep(200);
        Set<String> redisSessionKeysCleanup = redisTemplate.keys(sessionNamespace + ":sessions:*");
        redisTemplate.delete(redisSessionKeysCleanup);

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

        MultiValueMap<String, ResponseCookie> responseCookies = responseSpec.returnResult(Void.class).getResponseCookies();
        assertEquals(1, responseCookies.size());
        String sessionCookie = responseCookies.get(sessionCookieName).get(0).getValue();

        webDriver.manage().addCookie(new Cookie(sessionCookieName, sessionCookie));
        webDriver.navigate().refresh();
        Thread.sleep(200);

        assertAll(
            () -> assertDoesNotThrow(() -> webDriver.findElement(By.id("logout_button"))),
            () -> {
                WebElement logoutButton = webDriver.findElement(By.id("logout_button"));
                actions.moveToElement(logoutButton).click().perform();

                WebElement logoutModal = webDriver.findElement(By.id("logout_modal"));
                webDriverWait.until(driver -> logoutModal.isDisplayed());
                assertAll(
                    () -> {
                        WebElement logoutSubmit = logoutModal.findElement(By.id("logout_submit"));
                        actions.moveToElement(logoutSubmit).click().perform();

                        WebElement logoutResponse = logoutModal.findElement(By.id("logout_response"));
                        webDriverWait.until(driver -> !logoutResponse.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement logoutSuccessResponse = logoutResponse.findElement(By.id("logout_success"));
                                assertEquals(colors.get("SUCCESS"), Color.fromString(logoutSuccessResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(logoutSuccessResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement logoutSuccessResponseIcon = logoutSuccessResponse.findElement(By.tagName("i"));
                                        String logoutSuccessResponseIconClass = logoutSuccessResponseIcon.getAttribute("class");
                                        assertNotNull(logoutSuccessResponseIconClass);
                                        assertTrue(logoutSuccessResponseIconClass.contains("fa-solid fa-check"));
                                        assertEquals("Success!", logoutSuccessResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(1000);

                        assertAll(
                            () -> {
                                assertThrows(NoSuchElementException.class,
                                    () -> webDriver.findElement(By.id("logout_modal")));
                                assertThrows(NoSuchElementException.class,
                                    () -> webDriver.findElement(By.id("logout_button")));
                                assertDoesNotThrow(() -> webDriver.findElement(By.id("login_button")));
                                assertDoesNotThrow(() -> webDriver.findElement(By.id("register_button")));
                            }
                        );
                    }
                );
            },
            () -> {
                Set<String> redisSessionKeys = redisTemplate.keys(sessionNamespace + ":sessions:*");
                assertEquals(1, redisSessionKeys.size());
            }
        );

        webDriver.manage().deleteCookieNamed(sessionCookieName);
    }
}
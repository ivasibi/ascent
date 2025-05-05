package org.ascent.e2es;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
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
public class ViewE2ETest extends ContainerEnvironment {

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
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();

        lettuceConnectionFactory.start();
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

    private static Stream<Arguments> indexAppearance() {
        return Stream.of(
            arguments(WebBrowser.CHROME, 1920, 1080),
            arguments(WebBrowser.CHROME, 760, 1020),
            arguments(WebBrowser.CHROME, 360, 740),
            arguments(WebBrowser.FIREFOX, 1920, 1080),
            arguments(WebBrowser.FIREFOX, 760, 1020),
            arguments(WebBrowser.FIREFOX, 360, 740),
            arguments(WebBrowser.EDGE, 1920, 1080),
            arguments(WebBrowser.EDGE, 760, 1020),
            arguments(WebBrowser.EDGE, 360, 740)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexAppearance(WebBrowser webBrowser, int width, int height) {
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

        webDriver.manage().window().setSize(new Dimension(width, height));
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> assertEquals("Ascent", webDriver.getTitle()),
            () -> {
                WebElement body = webDriver.findElement(By.tagName("body"));
                assertEquals(colors.get("LIGHT"), Color.fromString(body.getCssValue("background-color")));
            },
            () -> {
                WebElement nav = webDriver.findElement(By.tagName("nav"));
                assertEquals(colors.get("DARK"), Color.fromString(nav.getCssValue("background-color")));
            }
        );
    }

    private static Stream<Arguments> indexMenuAppearance() {
        return Stream.of(
            arguments(WebBrowser.CHROME, 1920, 1080),
            arguments(WebBrowser.CHROME, 760, 1020),
            arguments(WebBrowser.CHROME, 360, 740),
            arguments(WebBrowser.FIREFOX, 1920, 1080),
            arguments(WebBrowser.FIREFOX, 760, 1020),
            arguments(WebBrowser.FIREFOX, 360, 740),
            arguments(WebBrowser.EDGE, 1920, 1080),
            arguments(WebBrowser.EDGE, 760, 1020),
            arguments(WebBrowser.EDGE, 360, 740)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexMenuAppearance(WebBrowser webBrowser, int width, int height) {
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

        webDriver.manage().window().setSize(new Dimension(width, height));
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> {
                WebElement menuButton = webDriver.findElement(By.id("menu_button"));
                assertEquals(colors.get("DARK"), Color.fromString(menuButton.getCssValue("background-color")));
                assertEquals(colors.get("LIGHT"), Color.fromString(menuButton.getCssValue("color")));
                assertAll(
                    () -> {
                        WebElement menuButtonIcon = menuButton.findElement(By.tagName("i"));
                        String menuButtonIconClass = menuButtonIcon.getAttribute("class");
                        assertNotNull(menuButtonIconClass);
                        assertTrue(menuButtonIconClass.contains("fa-solid fa-bars"));
                    },
                    () -> {
                        if (width >= 580) {
                            assertEquals("Menu", menuButton.getText());
                        } else {
                            assertTrue(menuButton.getText().isEmpty());
                        }
                    }
                );
            },
            () -> {
                WebElement menuButton = webDriver.findElement(By.id("menu_button"));
                actions.moveToElement(menuButton).perform();
                Thread.sleep(200);
                assertEquals(colors.get("DARK_HOVER"), Color.fromString(menuButton.getCssValue("background-color")));
                actions.moveToElement(menuButton).click().perform();

                WebElement menuOffcanvas = webDriver.findElement(By.id("menu_offcanvas"));
                webDriverWait.until(driver -> menuOffcanvas.isDisplayed());
                Thread.sleep(200);
            },
            () -> {
                WebElement menuOffcanvas = webDriver.findElement(By.id("menu_offcanvas"));
                assertEquals(colors.get("DARK"), Color.fromString(menuOffcanvas.getCssValue("background-color")));
                assertAll(
                    () -> {
                        WebElement menuClose = menuOffcanvas.findElement(By.id("menu_close"));
                        assertEquals(colors.get("DARK"), Color.fromString(menuClose.getCssValue("background-color")));
                        assertEquals(colors.get("LIGHT"), Color.fromString(menuClose.getCssValue("color")));
                        assertAll(
                            () -> {
                                WebElement menuCloseIcon = menuClose.findElement(By.tagName("i"));
                                String menuCloseIconClass = menuCloseIcon.getAttribute("class");
                                assertNotNull(menuCloseIconClass);
                                assertTrue(menuCloseIconClass.contains("fa-solid fa-xmark"));
                                assertTrue(menuClose.getText().isEmpty());
                            }
                        );
                    },
                    () -> {
                        WebElement menuClose = menuOffcanvas.findElement(By.id("menu_close"));
                        actions.moveToElement(menuClose).perform();
                        Thread.sleep(200);
                        assertEquals(colors.get("DARK_HOVER"), Color.fromString(menuClose.getCssValue("background-color")));
                    },
                    () -> {
                        WebElement homeLink = menuOffcanvas.findElement(By.id("home_link"));
                        assertEquals(colors.get("PRIMARY"), Color.fromString(homeLink.getCssValue("background-color")));
                        assertEquals(colors.get("LIGHT"), Color.fromString(homeLink.getCssValue("color")));
                        String homeLinkClass = homeLink.getAttribute("class");
                        assertNotNull(homeLinkClass);
                        assertTrue(homeLinkClass.contains("active"));
                        assertAll(
                            () -> {
                                WebElement homeLinkIcon = homeLink.findElement(By.tagName("i"));
                                String homeLinkIconClass = homeLinkIcon.getAttribute("class");
                                assertNotNull(homeLinkIconClass);
                                assertTrue(homeLinkIconClass.contains("fa-solid fa-house"));
                                assertEquals("Home", homeLink.getText());
                            }
                        );
                    },
                    () -> {
                        WebElement placesLink = menuOffcanvas.findElement(By.id("places_link"));
                        assertEquals(colors.get("LIGHT"), Color.fromString(placesLink.getCssValue("color")));
                        assertAll(
                            () -> {
                                WebElement placesLinkIcon = placesLink.findElement(By.tagName("i"));
                                String placesLinkIconClass = placesLinkIcon.getAttribute("class");
                                assertNotNull(placesLinkIconClass);
                                assertTrue(placesLinkIconClass.contains("fa-solid fa-location-dot"));
                                assertEquals("Places", placesLink.getText());
                            }
                        );
                    },
                    () -> {
                        WebElement menuClose = menuOffcanvas.findElement(By.id("menu_close"));
                        actions.moveToElement(menuClose).click().perform();

                        webDriverWait.until(driver -> !menuOffcanvas.isDisplayed());
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexRegisterAppearance() {
        return Stream.of(
            arguments(WebBrowser.CHROME, 1920, 1080),
            arguments(WebBrowser.CHROME, 760, 1020),
            arguments(WebBrowser.CHROME, 360, 740),
            arguments(WebBrowser.FIREFOX, 1920, 1080),
            arguments(WebBrowser.FIREFOX, 760, 1020),
            arguments(WebBrowser.FIREFOX, 360, 740),
            arguments(WebBrowser.EDGE, 1920, 1080),
            arguments(WebBrowser.EDGE, 760, 1020),
            arguments(WebBrowser.EDGE, 360, 740)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexRegisterAppearance(WebBrowser webBrowser, int width, int height) {
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

        webDriver.manage().window().setSize(new Dimension(width, height));
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> {
                WebElement registerButton = webDriver.findElement(By.id("register_button"));
                assertEquals(colors.get("PRIMARY"), Color.fromString(registerButton.getCssValue("border-color")));
                assertEquals(colors.get("LIGHT"), Color.fromString(registerButton.getCssValue("color")));
                assertAll(
                    () -> {
                        WebElement registerButtonIcon = registerButton.findElement(By.tagName("i"));
                        String registerButtonIconClass = registerButtonIcon.getAttribute("class");
                        assertNotNull(registerButtonIconClass);
                        assertTrue(registerButtonIconClass.contains("fa-solid fa-user"));
                    },
                    () -> {
                        if (width >= 580) {
                            assertEquals("Register", registerButton.getText());
                        } else {
                            assertTrue(registerButton.getText().isEmpty());
                        }
                    }
                );
            },
            () -> {
                WebElement registerButton = webDriver.findElement(By.id("register_button"));
                actions.moveToElement(registerButton).perform();
                Thread.sleep(200);
                assertEquals(colors.get("PRIMARY_OUTLINE_HOVER"), Color.fromString(registerButton.getCssValue("background-color")));
                actions.moveToElement(registerButton).click().perform();

                WebElement registerModal = webDriver.findElement(By.id("register_modal"));
                webDriverWait.until(driver -> registerModal.isDisplayed());
                Thread.sleep(200);
            },
            () -> {
                WebElement registerModal = webDriver.findElement(By.id("register_modal"));
                assertAll(
                    () -> {
                        WebElement registerModalHeader = registerModal.findElement(By.className("modal-header"));
                        assertEquals(colors.get("PRIMARY"), Color.fromString(registerModalHeader.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement registerTitle = registerModalHeader.findElement(By.tagName("h4"));
                                assertAll(
                                    () -> {
                                        WebElement registerTitleIcon = registerTitle.findElement(By.tagName("i"));
                                        String registerTitleIconClass = registerTitleIcon.getAttribute("class");
                                        assertNotNull(registerTitleIconClass);
                                        assertTrue(registerTitleIconClass.contains("fa-solid fa-user"));
                                        assertEquals("Register", registerTitle.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerClose = registerModalHeader.findElement(By.id("register_close"));
                                assertEquals(colors.get("PRIMARY"), Color.fromString(registerClose.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerClose.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerCloseIcon = registerClose.findElement(By.tagName("i"));
                                        String registerCloseIconClass = registerCloseIcon.getAttribute("class");
                                        assertNotNull(registerCloseIconClass);
                                        assertTrue(registerCloseIconClass.contains("fa-solid fa-xmark"));
                                        assertTrue(registerClose.getText().isEmpty());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerClose = registerModalHeader.findElement(By.id("register_close"));
                                actions.moveToElement(registerClose).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("PRIMARY_HOVER"), Color.fromString(registerClose.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement registerModalBody = registerModal.findElement(By.className("modal-body"));
                        assertEquals(colors.get("DARK"), Color.fromString(registerModalBody.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement registerUsernameLabel = registerModalBody.findElement(By.cssSelector("label[for=register_username]"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerUsernameLabel.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerUsernameLabelIcon = registerUsernameLabel.findElement(By.tagName("i"));
                                        String registerUsernameLabelIconClass = registerUsernameLabelIcon.getAttribute("class");
                                        assertNotNull(registerUsernameLabelIconClass);
                                        assertTrue(registerUsernameLabelIconClass.contains("fa-solid fa-user"));
                                        assertEquals("Username", registerUsernameLabel.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerUsernameInput = registerModalBody.findElement(By.id("register_username"));
                                assertEquals(colors.get("DARK"), Color.fromString(registerUsernameInput.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerUsernameInput.getCssValue("color")));
                                assertEquals("text", registerUsernameInput.getAttribute("type"));
                            },
                            () -> {
                                WebElement registerUsernameInput = registerModalBody.findElement(By.id("register_username"));
                                assertEquals(registerUsernameInput, webDriver.switchTo().activeElement());
                            },
                            () -> {
                                WebElement registerEmailLabel = registerModalBody.findElement(By.cssSelector("label[for=register_email]"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerEmailLabel.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerEmailLabelIcon = registerEmailLabel.findElement(By.tagName("i"));
                                        String registerEmailLabelIconClass = registerEmailLabelIcon.getAttribute("class");
                                        assertNotNull(registerEmailLabelIconClass);
                                        assertTrue(registerEmailLabelIconClass.contains("fa-solid fa-envelope"));
                                        assertEquals("Email", registerEmailLabel.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerEmailInput = registerModalBody.findElement(By.id("register_email"));
                                assertEquals(colors.get("DARK"), Color.fromString(registerEmailInput.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerEmailInput.getCssValue("color")));
                                assertEquals("email", registerEmailInput.getAttribute("type"));
                            },
                            () -> {
                                WebElement registerPasswordLabel = registerModalBody.findElement(By.cssSelector("label[for=register_password]"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerPasswordLabel.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerPasswordLabelIcon = registerPasswordLabel.findElement(By.tagName("i"));
                                        String registerPasswordLabelIconClass = registerPasswordLabelIcon.getAttribute("class");
                                        assertNotNull(registerPasswordLabelIconClass);
                                        assertTrue(registerPasswordLabelIconClass.contains("fa-solid fa-key"));
                                        assertEquals("Password", registerPasswordLabel.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerPasswordInput = registerModalBody.findElement(By.id("register_password"));
                                assertEquals(colors.get("DARK"), Color.fromString(registerPasswordInput.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerPasswordInput.getCssValue("color")));
                                assertEquals("password", registerPasswordInput.getAttribute("type"));
                            },
                            () -> {
                                WebElement registerResponse = registerModalBody.findElement(By.id("register_response"));
                                String registerResponseInnerHTML = registerResponse.getAttribute("innerHTML");
                                assertNotNull(registerResponseInnerHTML);
                                assertTrue(registerResponseInnerHTML.isEmpty());
                            },
                            () -> {
                                WebElement registerError = registerModalBody.findElement(By.id("register_error"));
                                String registerErrorInnerHTML = registerError.getAttribute("innerHTML");
                                assertNotNull(registerErrorInnerHTML);
                                assertTrue(registerErrorInnerHTML.isEmpty());
                            }
                        );
                    },
                    () -> {
                        WebElement registerModalFooter = registerModal.findElement(By.className("modal-footer"));
                        assertEquals(colors.get("DARK"), Color.fromString(registerModalFooter.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement registerIndicator = registerModalFooter.findElement(By.className("htmx-indicator"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerIndicator.getCssValue("color")));
                                assertFalse(registerIndicator.isDisplayed());
                            },
                            () -> {
                                WebElement registerSubmit = registerModalFooter.findElement(By.id("register_submit"));
                                assertEquals(colors.get("PRIMARY"), Color.fromString(registerSubmit.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerSubmit.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerSubmitIcon = registerSubmit.findElement(By.tagName("i"));
                                        String registerSubmitIconClass = registerSubmitIcon.getAttribute("class");
                                        assertNotNull(registerSubmitIconClass);
                                        assertTrue(registerSubmitIconClass.contains("fa-solid fa-user"));
                                        assertEquals("Register", registerSubmit.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement registerSubmit = registerModalFooter.findElement(By.id("register_submit"));
                                actions.moveToElement(registerSubmit).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("PRIMARY_HOVER"), Color.fromString(registerSubmit.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement registerClose = registerModal.findElement(By.id("register_close"));
                        actions.moveToElement(registerClose).click().perform();

                        webDriverWait.until(driver -> !registerModal.isDisplayed());
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLoginAppearance() {
        return Stream.of(
            arguments(WebBrowser.CHROME, 1920, 1080),
            arguments(WebBrowser.CHROME, 760, 1020),
            arguments(WebBrowser.CHROME, 360, 740),
            arguments(WebBrowser.FIREFOX, 1920, 1080),
            arguments(WebBrowser.FIREFOX, 760, 1020),
            arguments(WebBrowser.FIREFOX, 360, 740),
            arguments(WebBrowser.EDGE, 1920, 1080),
            arguments(WebBrowser.EDGE, 760, 1020),
            arguments(WebBrowser.EDGE, 360, 740)
        );
    }

    public void indexLoginAppearance(WebBrowser webBrowser, int width, int height) {
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

        webDriver.manage().window().setSize(new Dimension(width, height));
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

        assertAll(
            () -> {
                WebElement loginButton = webDriver.findElement(By.id("login_button"));
                assertEquals(colors.get("SUCCESS"), Color.fromString(loginButton.getCssValue("border-color")));
                assertEquals(colors.get("LIGHT"), Color.fromString(loginButton.getCssValue("color")));
                assertAll(
                    () -> {
                        WebElement loginButtonIcon = loginButton.findElement(By.tagName("i"));
                        String loginButtonIconClass = loginButtonIcon.getAttribute("class");
                        assertNotNull(loginButtonIconClass);
                        assertTrue(loginButtonIconClass.contains("fa-solid fa-right-to-bracket"));
                    },
                    () -> {
                        if (width >= 580) {
                            assertEquals("Login", loginButton.getText());
                        } else {
                            assertTrue(loginButton.getText().isEmpty());
                        }
                    }
                );
            },
            () -> {
                WebElement loginButton = webDriver.findElement(By.id("login_button"));
                actions.moveToElement(loginButton).perform();
                Thread.sleep(200);
                assertEquals(colors.get("SUCCESS_OUTLINE_HOVER"), Color.fromString(loginButton.getCssValue("background-color")));
                actions.moveToElement(loginButton).click().perform();

                WebElement loginModal = webDriver.findElement(By.id("login_modal"));
                webDriverWait.until(driver -> loginModal.isDisplayed());
                Thread.sleep(200);
            },
            () -> {
                WebElement loginModal = webDriver.findElement(By.id("login_modal"));
                assertAll(
                    () -> {
                        WebElement loginModalHeader = loginModal.findElement(By.className("modal-header"));
                        assertEquals(colors.get("SUCCESS"), Color.fromString(loginModalHeader.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement loginTitle = loginModalHeader.findElement(By.tagName("h4"));
                                assertAll(
                                    () -> {
                                        WebElement loginTitleIcon = loginTitle.findElement(By.tagName("i"));
                                        String loginTitleIconClass = loginTitleIcon.getAttribute("class");
                                        assertNotNull(loginTitleIconClass);
                                        assertTrue(loginTitleIconClass.contains("fa-solid fa-right-to-bracket"));
                                        assertEquals("Login", loginTitle.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement loginClose = loginModalHeader.findElement(By.id("login_close"));
                                assertEquals(colors.get("SUCCESS"), Color.fromString(loginClose.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginClose.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginCloseIcon = loginClose.findElement(By.tagName("i"));
                                        String loginCloseIconClass = loginCloseIcon.getAttribute("class");
                                        assertNotNull(loginCloseIconClass);
                                        assertTrue(loginCloseIconClass.contains("fa-solid fa-xmark"));
                                        assertTrue(loginClose.getText().isEmpty());
                                    }
                                );
                            },
                            () -> {
                                WebElement loginClose = loginModalHeader.findElement(By.id("login_close"));
                                actions.moveToElement(loginClose).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("SUCCESS_HOVER"), Color.fromString(loginClose.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement loginModalBody = loginModal.findElement(By.className("modal-body"));
                        assertEquals(colors.get("DARK"), Color.fromString(loginModalBody.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement loginEmailLabel = loginModalBody.findElement(By.cssSelector("label[for=login_email]"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginEmailLabel.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginEmailLabelIcon = loginEmailLabel.findElement(By.tagName("i"));
                                        String loginEmailLabelIconClass = loginEmailLabelIcon.getAttribute("class");
                                        assertNotNull(loginEmailLabelIconClass);
                                        assertTrue(loginEmailLabelIconClass.contains("fa-solid fa-envelope"));
                                        assertEquals("Email", loginEmailLabel.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement loginEmailInput = loginModalBody.findElement(By.id("login_email"));
                                assertEquals(colors.get("DARK"), Color.fromString(loginEmailInput.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginEmailInput.getCssValue("color")));
                                assertEquals("email", loginEmailInput.getAttribute("type"));
                            },
                            () -> {
                                WebElement loginEmailInput = loginModalBody.findElement(By.id("login_email"));
                                assertEquals(loginEmailInput, webDriver.switchTo().activeElement());
                            },
                            () -> {
                                WebElement loginPasswordLabel = loginModalBody.findElement(By.cssSelector("label[for=login_password]"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginPasswordLabel.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginPasswordLabelIcon = loginPasswordLabel.findElement(By.tagName("i"));
                                        String loginPasswordLabelIconClass = loginPasswordLabelIcon.getAttribute("class");
                                        assertNotNull(loginPasswordLabelIconClass);
                                        assertTrue(loginPasswordLabelIconClass.contains("fa-solid fa-key"));
                                        assertEquals("Password", loginPasswordLabel.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement loginPasswordInput = loginModal.findElement(By.id("login_password"));
                                assertEquals(colors.get("DARK"), Color.fromString(loginPasswordInput.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginPasswordInput.getCssValue("color")));
                                assertEquals("password", loginPasswordInput.getAttribute("type"));
                            },
                            () -> {
                                WebElement loginResponse = loginModalBody.findElement(By.id("login_response"));
                                String loginResponseInnerHTML = loginResponse.getAttribute("innerHTML");
                                assertNotNull(loginResponseInnerHTML);
                                assertTrue(loginResponseInnerHTML.isEmpty());
                            },
                            () -> {
                                WebElement loginError = loginModalBody.findElement(By.id("login_error"));
                                String loginErrorInnerHTML = loginError.getAttribute("innerHTML");
                                assertNotNull(loginErrorInnerHTML);
                                assertTrue(loginErrorInnerHTML.isEmpty());
                            }
                        );
                    },
                    () -> {
                        WebElement loginModalFooter = loginModal.findElement(By.className("modal-footer"));
                        assertEquals(colors.get("DARK"), Color.fromString(loginModalFooter.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement loginIndicator = loginModalFooter.findElement(By.className("htmx-indicator"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginIndicator.getCssValue("color")));
                                assertFalse(loginIndicator.isDisplayed());
                            },
                            () -> {
                                WebElement loginSubmit = loginModalFooter.findElement(By.id("login_submit"));
                                assertEquals(colors.get("SUCCESS"), Color.fromString(loginSubmit.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginSubmit.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginSubmitIcon = loginSubmit.findElement(By.tagName("i"));
                                        String loginSubmitIconClass = loginSubmitIcon.getAttribute("class");
                                        assertNotNull(loginSubmitIconClass);
                                        assertTrue(loginSubmitIconClass.contains("fa-solid fa-right-to-bracket"));
                                        assertEquals("Login", loginSubmit.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement loginSubmit = loginModalFooter.findElement(By.id("login_submit"));
                                actions.moveToElement(loginSubmit).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("SUCCESS_HOVER"), Color.fromString(loginSubmit.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement loginClose = loginModal.findElement(By.id("login_close"));
                        actions.moveToElement(loginClose).click().perform();

                        webDriverWait.until(driver -> !loginModal.isDisplayed());
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLogoutAppearance() {
        return Stream.of(
            arguments(WebBrowser.CHROME, 1920, 1080, "username@email.com", "password"),
            arguments(WebBrowser.CHROME, 1920, 1080, "username2@email.com", "password2"),
            arguments(WebBrowser.CHROME, 1920, 1080, "username3@email.com", "password3"),
            arguments(WebBrowser.CHROME, 1920, 1080, "username4@email.com", "password4"),
            arguments(WebBrowser.CHROME, 760, 1020, "username@email.com", "password"),
            arguments(WebBrowser.CHROME, 760, 1020, "username2@email.com", "password2"),
            arguments(WebBrowser.CHROME, 760, 1020, "username3@email.com", "password3"),
            arguments(WebBrowser.CHROME, 760, 1020, "username4@email.com", "password4"),
            arguments(WebBrowser.CHROME, 360, 740, "username@email.com", "password"),
            arguments(WebBrowser.CHROME, 360, 740, "username2@email.com", "password2"),
            arguments(WebBrowser.CHROME, 360, 740, "username3@email.com", "password3"),
            arguments(WebBrowser.CHROME, 360, 740, "username4@email.com", "password4"),
            arguments(WebBrowser.FIREFOX, 1920, 1080, "username@email.com", "password"),
            arguments(WebBrowser.FIREFOX, 1920, 1080, "username2@email.com", "password2"),
            arguments(WebBrowser.FIREFOX, 1920, 1080, "username3@email.com", "password3"),
            arguments(WebBrowser.FIREFOX, 1920, 1080, "username4@email.com", "password4"),
            arguments(WebBrowser.FIREFOX, 760, 1020, "username@email.com", "password"),
            arguments(WebBrowser.FIREFOX, 760, 1020, "username2@email.com", "password2"),
            arguments(WebBrowser.FIREFOX, 760, 1020, "username3@email.com", "password3"),
            arguments(WebBrowser.FIREFOX, 760, 1020, "username4@email.com", "password4"),
            arguments(WebBrowser.FIREFOX, 360, 740, "username@email.com", "password"),
            arguments(WebBrowser.FIREFOX, 360, 740, "username2@email.com", "password2"),
            arguments(WebBrowser.FIREFOX, 360, 740, "username3@email.com", "password3"),
            arguments(WebBrowser.FIREFOX, 360, 740, "username4@email.com", "password4"),
            arguments(WebBrowser.EDGE, 1920, 1080, "username@email.com", "password"),
            arguments(WebBrowser.EDGE, 1920, 1080, "username2@email.com", "password2"),
            arguments(WebBrowser.EDGE, 1920, 1080, "username3@email.com", "password3"),
            arguments(WebBrowser.EDGE, 1920, 1080, "username4@email.com", "password4"),
            arguments(WebBrowser.EDGE, 760, 1020, "username@email.com", "password"),
            arguments(WebBrowser.EDGE, 760, 1020, "username2@email.com", "password2"),
            arguments(WebBrowser.EDGE, 760, 1020, "username3@email.com", "password3"),
            arguments(WebBrowser.EDGE, 760, 1020, "username4@email.com", "password4"),
            arguments(WebBrowser.EDGE, 360, 740, "username@email.com", "password"),
            arguments(WebBrowser.EDGE, 360, 740, "username2@email.com", "password2"),
            arguments(WebBrowser.EDGE, 360, 740, "username3@email.com", "password3"),
            arguments(WebBrowser.EDGE, 360, 740, "username4@email.com", "password4")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLogoutAppearance(WebBrowser webBrowser, int width, int height, String email, String password) throws Exception {
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

        webDriver.manage().window().setSize(new Dimension(width, height));
        webDriver.get(serverProtocol + serverContainerHostName + ":" + serverHostPort + "/");

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
            () -> {
                WebElement logoutButton = webDriver.findElement(By.id("logout_button"));
                assertEquals(colors.get("DANGER"), Color.fromString(logoutButton.getCssValue("border-color")));
                assertEquals(colors.get("LIGHT"), Color.fromString(logoutButton.getCssValue("color")));
                assertAll(
                    () -> {
                        WebElement logoutButtonIcon = logoutButton.findElement(By.tagName("i"));
                        String logoutButtonIconClass = logoutButtonIcon.getAttribute("class");
                        assertNotNull(logoutButtonIconClass);
                        assertTrue(logoutButtonIconClass.contains("fa-solid fa-right-from-bracket"));
                    },
                    () -> {
                        if (width >= 580) {
                            assertEquals("Logout", logoutButton.getText());
                        } else {
                            assertTrue(logoutButton.getText().isEmpty());
                        }
                    }
                );
            },
            () -> {
                WebElement logoutButton = webDriver.findElement(By.id("logout_button"));
                actions.moveToElement(logoutButton).perform();
                Thread.sleep(200);
                assertEquals(colors.get("DANGER_OUTLINE_HOVER"), Color.fromString(logoutButton.getCssValue("background-color")));
                actions.moveToElement(logoutButton).click().perform();

                WebElement logoutModal = webDriver.findElement(By.id("logout_modal"));
                webDriverWait.until(driver -> logoutModal.isDisplayed());
                Thread.sleep(200);
            },
            () -> {
                WebElement logoutModal = webDriver.findElement(By.id("logout_modal"));
                assertAll(
                    () -> {
                        WebElement logoutModalHeader = logoutModal.findElement(By.className("modal-header"));
                        assertEquals(colors.get("DANGER"), Color.fromString(logoutModalHeader.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement logoutTitle = logoutModalHeader.findElement(By.tagName("h4"));
                                assertAll(
                                    () -> {
                                        WebElement logoutTitleIcon = logoutTitle.findElement(By.tagName("i"));
                                        String logoutTitleIconClass = logoutTitleIcon.getAttribute("class");
                                        assertNotNull(logoutTitleIconClass);
                                        assertTrue(logoutTitleIconClass.contains("fa-solid fa-right-from-bracket"));
                                        assertEquals("Logout", logoutTitle.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement logoutClose = logoutModalHeader.findElement(By.id("logout_close"));
                                assertEquals(colors.get("DANGER"), Color.fromString(logoutClose.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(logoutClose.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement logoutCloseIcon = logoutClose.findElement(By.tagName("i"));
                                        String logoutCloseIconClass = logoutCloseIcon.getAttribute("class");
                                        assertNotNull(logoutCloseIconClass);
                                        assertTrue(logoutCloseIconClass.contains("fa-solid fa-xmark"));
                                        assertTrue(logoutClose.getText().isEmpty());
                                    }
                                );
                            },
                            () -> {
                                WebElement logoutClose = logoutModalHeader.findElement(By.id("logout_close"));
                                actions.moveToElement(logoutClose).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("DANGER_HOVER"), Color.fromString(logoutClose.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement logoutModalBody = logoutModal.findElement(By.className("modal-body"));
                        assertEquals(colors.get("DARK"), Color.fromString(logoutModalBody.getCssValue("background-color")));
                        assertEquals(colors.get("LIGHT"), Color.fromString(logoutModalBody.getCssValue("color")));
                        assertEquals("Are you sure you want to logout?", logoutModalBody.getText());
                        assertAll(
                            () -> {
                                WebElement logoutResponse = logoutModalBody.findElement(By.id("logout_response"));
                                String logoutResponseInnerHTML = logoutResponse.getAttribute("innerHTML");
                                assertNotNull(logoutResponseInnerHTML);
                                assertTrue(logoutResponseInnerHTML.isEmpty());
                            },
                            () -> {
                                WebElement logoutError = logoutModalBody.findElement(By.id("logout_error"));
                                String logoutErrorInnerHTML = logoutError.getAttribute("innerHTML");
                                assertNotNull(logoutErrorInnerHTML);
                                assertTrue(logoutErrorInnerHTML.isEmpty());
                            }
                        );
                    },
                    () -> {
                        WebElement logoutModalFooter = logoutModal.findElement(By.className("modal-footer"));
                        assertEquals(colors.get("DARK"), Color.fromString(logoutModalFooter.getCssValue("background-color")));
                        assertAll(
                            () -> {
                                WebElement logoutIndicator = logoutModalFooter.findElement(By.className("htmx-indicator"));
                                assertEquals(colors.get("LIGHT"), Color.fromString(logoutIndicator.getCssValue("color")));
                                assertFalse(logoutIndicator.isDisplayed());
                            },
                            () -> {
                                WebElement logoutSubmit = logoutModalFooter.findElement(By.id("logout_submit"));
                                assertEquals(colors.get("DANGER"), Color.fromString(logoutSubmit.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(logoutSubmit.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement logoutSubmitIcon = logoutSubmit.findElement(By.tagName("i"));
                                        String logoutSubmitIconClass = logoutSubmitIcon.getAttribute("class");
                                        assertNotNull(logoutSubmitIconClass);
                                        assertTrue(logoutSubmitIconClass.contains("fa-solid fa-right-from-bracket"));
                                        assertEquals("Logout", logoutSubmit.getText());
                                    }
                                );
                            },
                            () -> {
                                WebElement logoutSubmit = logoutModalFooter.findElement(By.id("logout_submit"));
                                actions.moveToElement(logoutSubmit).perform();
                                Thread.sleep(200);
                                assertEquals(colors.get("DANGER_HOVER"), Color.fromString(logoutSubmit.getCssValue("background-color")));
                            }
                        );
                    },
                    () -> {
                        WebElement logoutClose = logoutModal.findElement(By.id("logout_close"));
                        actions.moveToElement(logoutClose).click().perform();

                        webDriverWait.until(driver -> !logoutModal.isDisplayed());
                    }
                );
            }
        );

        webDriver.manage().deleteCookieNamed(sessionCookieName);
    }

    private static Stream<Arguments> indexRegisterInteractionWithNonExistingUser() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.CHROME, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username8", "username8@email.com", "password8"),
            arguments(WebBrowser.CHROME, "username9", "username9@email.com", "password9"),
            arguments(WebBrowser.FIREFOX, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.FIREFOX, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username8", "username8@email.com", "password8"),
            arguments(WebBrowser.FIREFOX, "username9", "username9@email.com", "password9"),
            arguments(WebBrowser.EDGE, "username6", "username6@email.com", "password6"),
            arguments(WebBrowser.EDGE, "username7", "username7@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username8", "username8@email.com", "password8"),
            arguments(WebBrowser.EDGE, "username9", "username9@email.com", "password9")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexRegisterInteractionWithNonExistingUser(WebBrowser webBrowser, String username, String email, String password) {
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
            }
        );
    }

    private static Stream<Arguments> indexRegisterInteractionWithExistingUsername() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username", "username6@email.com", "password6"),
            arguments(WebBrowser.CHROME, "username", "username7@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username", "username8@email.com", "password8"),
            arguments(WebBrowser.CHROME, "username", "username9@email.com", "password9"),
            arguments(WebBrowser.FIREFOX, "username", "username6@email.com", "password6"),
            arguments(WebBrowser.FIREFOX, "username", "username7@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username", "username8@email.com", "password8"),
            arguments(WebBrowser.FIREFOX, "username", "username9@email.com", "password9"),
            arguments(WebBrowser.EDGE, "username", "username6@email.com", "password6"),
            arguments(WebBrowser.EDGE, "username", "username7@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username", "username8@email.com", "password8"),
            arguments(WebBrowser.EDGE, "username", "username9@email.com", "password9")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexRegisterInteractionWithExistingUsername(WebBrowser webBrowser, String username, String email, String password) {
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

                        WebElement registerError = registerModal.findElement(By.id("register_error"));
                        webDriverWait.until(driver -> !registerError.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement registerUsernameAlreadyInUseResponse = registerError.findElement(By.id("register_username_already_in_use"));
                                assertEquals(colors.get("DANGER"), Color.fromString(registerUsernameAlreadyInUseResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerUsernameAlreadyInUseResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerUsernameAlreadyInUseResponseIcon = registerUsernameAlreadyInUseResponse.findElement(By.tagName("i"));
                                        String registerUsernameAlreadyInUseResponseIconClass = registerUsernameAlreadyInUseResponseIcon.getAttribute("class");
                                        assertNotNull(registerUsernameAlreadyInUseResponseIconClass);
                                        assertTrue(registerUsernameAlreadyInUseResponseIconClass.contains("fa-solid fa-xmark"));
                                        assertEquals("Username is already in use!", registerUsernameAlreadyInUseResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(200);

                        assertAll(
                            () -> {
                                String registerUsernameValue = registerUsername.getAttribute("value");
                                assertNotNull(registerUsernameValue);
                                assertFalse(registerUsernameValue.isEmpty());
                            },
                            () -> {
                                String registerEmailValue = registerEmail.getAttribute("value");
                                assertNotNull(registerEmailValue);
                                assertFalse(registerEmailValue.isEmpty());
                            },
                            () -> {
                                String registerPasswordValue = registerPassword.getAttribute("value");
                                assertNotNull(registerPasswordValue);
                                assertFalse(registerPasswordValue.isEmpty());
                            },
                            () -> {
                                String registerErrorInnerHTML = registerError.getAttribute("innerHTML");
                                assertNotNull(registerErrorInnerHTML);
                                assertFalse(registerErrorInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexRegisterInteractionWithExistingEmail() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username6", "username@email.com", "password6"),
            arguments(WebBrowser.CHROME, "username7", "username@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username8", "username@email.com", "password8"),
            arguments(WebBrowser.CHROME, "username9", "username@email.com", "password9"),
            arguments(WebBrowser.FIREFOX, "username6", "username@email.com", "password6"),
            arguments(WebBrowser.FIREFOX, "username7", "username@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username8", "username@email.com", "password8"),
            arguments(WebBrowser.FIREFOX, "username9", "username@email.com", "password9"),
            arguments(WebBrowser.EDGE, "username6", "username@email.com", "password6"),
            arguments(WebBrowser.EDGE, "username7", "username@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username8", "username@email.com", "password8"),
            arguments(WebBrowser.EDGE, "username9", "username@email.com", "password9")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexRegisterInteractionWithExistingEmail(WebBrowser webBrowser, String username, String email, String password) {
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

                        WebElement registerError = registerModal.findElement(By.id("register_error"));
                        webDriverWait.until(driver -> !registerError.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement registerEmailAlreadyInUseResponse = registerError.findElement(By.id("register_email_already_in_use"));
                                assertEquals(colors.get("DANGER"), Color.fromString(registerEmailAlreadyInUseResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(registerEmailAlreadyInUseResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement registerEmailAlreadyInUseResponseIcon = registerEmailAlreadyInUseResponse.findElement(By.tagName("i"));
                                        String registerEmailAlreadyInUseResponseIconClass = registerEmailAlreadyInUseResponseIcon.getAttribute("class");
                                        assertNotNull(registerEmailAlreadyInUseResponseIconClass);
                                        assertTrue(registerEmailAlreadyInUseResponseIconClass.contains("fa-solid fa-xmark"));
                                        assertEquals("Email is already in use!", registerEmailAlreadyInUseResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(200);

                        assertAll(
                            () -> {
                                String registerUsernameValue = registerUsername.getAttribute("value");
                                assertNotNull(registerUsernameValue);
                                assertFalse(registerUsernameValue.isEmpty());
                            },
                            () -> {
                                String registerEmailValue = registerEmail.getAttribute("value");
                                assertNotNull(registerEmailValue);
                                assertFalse(registerEmailValue.isEmpty());
                            },
                            () -> {
                                String registerPasswordValue = registerPassword.getAttribute("value");
                                assertNotNull(registerPasswordValue);
                                assertFalse(registerPasswordValue.isEmpty());
                            },
                            () -> {
                                String registerErrorInnerHTML = registerError.getAttribute("innerHTML");
                                assertNotNull(registerErrorInnerHTML);
                                assertFalse(registerErrorInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLoginInteractionWithExistingUser() {
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
    public void indexLoginInteractionWithExistingUser(WebBrowser webBrowser, String email, String password) {
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
            }
        );
    }

    private static Stream<Arguments> indexLoginInteractionWithNonExistingUser() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username6@email.com", "password6"),
            arguments(WebBrowser.CHROME, "username7@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username7@email.com", "password7"),
            arguments(WebBrowser.CHROME, "username8@email.com", "password8"),
            arguments(WebBrowser.FIREFOX, "username6@email.com", "password6"),
            arguments(WebBrowser.FIREFOX, "username7@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username7@email.com", "password7"),
            arguments(WebBrowser.FIREFOX, "username8@email.com", "password8"),
            arguments(WebBrowser.EDGE, "username6@email.com", "password6"),
            arguments(WebBrowser.EDGE, "username7@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username7@email.com", "password7"),
            arguments(WebBrowser.EDGE, "username8@email.com", "password8")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLoginInteractionWithNonExistingUser(WebBrowser webBrowser, String email, String password) {
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

                        WebElement loginError = loginModal.findElement(By.id("login_error"));
                        webDriverWait.until(driver -> !loginError.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement loginInvalidCredentialsResponse = loginError.findElement(By.id("login_invalid_credentials"));
                                assertEquals(colors.get("DANGER"), Color.fromString(loginInvalidCredentialsResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginInvalidCredentialsResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginInvalidCredentialsResponseIcon = loginInvalidCredentialsResponse.findElement(By.tagName("i"));
                                        String loginInvalidCredentialsResponseIconClass = loginInvalidCredentialsResponseIcon.getAttribute("class");
                                        assertNotNull(loginInvalidCredentialsResponseIconClass);
                                        assertTrue(loginInvalidCredentialsResponseIconClass.contains("fa-solid fa-xmark"));
                                        assertEquals("Invalid credentials!", loginInvalidCredentialsResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(200);

                        assertAll(
                            () -> {
                                String loginEmailValue = loginEmail.getAttribute("value");
                                assertNotNull(loginEmailValue);
                                assertFalse(loginEmailValue.isEmpty());
                            },
                            () -> {
                                String loginPasswordValue = loginPassword.getAttribute("value");
                                assertNotNull(loginPasswordValue);
                                assertFalse(loginPasswordValue.isEmpty());
                            },
                            () -> {
                                String loginErrorInnerHTML = loginError.getAttribute("innerHTML");
                                assertNotNull(loginErrorInnerHTML);
                                assertFalse(loginErrorInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLoginInteractionWithDisabledUser() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username5@email.com", "password5"),
            arguments(WebBrowser.FIREFOX, "username5@email.com", "password5"),
            arguments(WebBrowser.EDGE, "username5@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLoginInteractionWithDisabledUser(WebBrowser webBrowser, String email, String password) {
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

                        WebElement loginError = loginModal.findElement(By.id("login_error"));
                        webDriverWait.until(driver -> !loginError.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement loginUserDisabledResponse = loginError.findElement(By.id("login_user_disabled"));
                                assertEquals(colors.get("DANGER"), Color.fromString(loginUserDisabledResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginUserDisabledResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginUserDisabledResponseIcon = loginUserDisabledResponse.findElement(By.tagName("i"));
                                        String loginUserDisabledResponseIconClass = loginUserDisabledResponseIcon.getAttribute("class");
                                        assertNotNull(loginUserDisabledResponseIconClass);
                                        assertTrue(loginUserDisabledResponseIconClass.contains("fa-solid fa-xmark"));
                                        assertEquals("User disabled!", loginUserDisabledResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(200);

                        assertAll(
                            () -> {
                                String loginEmailValue = loginEmail.getAttribute("value");
                                assertNotNull(loginEmailValue);
                                assertFalse(loginEmailValue.isEmpty());
                            },
                            () -> {
                                String loginPasswordValue = loginPassword.getAttribute("value");
                                assertNotNull(loginPasswordValue);
                                assertFalse(loginPasswordValue.isEmpty());
                            },
                            () -> {
                                String loginErrorInnerHTML = loginError.getAttribute("innerHTML");
                                assertNotNull(loginErrorInnerHTML);
                                assertFalse(loginErrorInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLoginInteractionWithNonMatchingPassword() {
        return Stream.of(
            arguments(WebBrowser.CHROME, "username@email.com", "password2"),
            arguments(WebBrowser.CHROME, "username2@email.com", "password3"),
            arguments(WebBrowser.CHROME, "username3@email.com", "password4"),
            arguments(WebBrowser.CHROME, "username4@email.com", "password5"),
            arguments(WebBrowser.FIREFOX, "username@email.com", "password2"),
            arguments(WebBrowser.FIREFOX, "username2@email.com", "password3"),
            arguments(WebBrowser.FIREFOX, "username3@email.com", "password4"),
            arguments(WebBrowser.FIREFOX, "username4@email.com", "password5"),
            arguments(WebBrowser.EDGE, "username@email.com", "password2"),
            arguments(WebBrowser.EDGE, "username2@email.com", "password3"),
            arguments(WebBrowser.EDGE, "username3@email.com", "password4"),
            arguments(WebBrowser.EDGE, "username4@email.com", "password5")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void indexLoginInteractionWithNonMatchingPassword(WebBrowser webBrowser, String email, String password) {
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

                        WebElement loginError = loginModal.findElement(By.id("login_error"));
                        webDriverWait.until(driver -> !loginError.getAttribute("innerHTML").isEmpty());
                        assertAll(
                            () -> {
                                WebElement loginInvalidCredentialsResponse = loginError.findElement(By.id("login_invalid_credentials"));
                                assertEquals(colors.get("DANGER"), Color.fromString(loginInvalidCredentialsResponse.getCssValue("background-color")));
                                assertEquals(colors.get("LIGHT"), Color.fromString(loginInvalidCredentialsResponse.getCssValue("color")));
                                assertAll(
                                    () -> {
                                        WebElement loginInvalidCredentialsResponseIcon = loginInvalidCredentialsResponse.findElement(By.tagName("i"));
                                        String loginInvalidCredentialsResponseIconClass = loginInvalidCredentialsResponseIcon.getAttribute("class");
                                        assertNotNull(loginInvalidCredentialsResponseIconClass);
                                        assertTrue(loginInvalidCredentialsResponseIconClass.contains("fa-solid fa-xmark"));
                                        assertEquals("Invalid credentials!", loginInvalidCredentialsResponse.getText());
                                    }
                                );
                            }
                        );

                        Thread.sleep(200);

                        assertAll(
                            () -> {
                                String loginEmailValue = loginEmail.getAttribute("value");
                                assertNotNull(loginEmailValue);
                                assertFalse(loginEmailValue.isEmpty());
                            },
                            () -> {
                                String loginPasswordValue = loginPassword.getAttribute("value");
                                assertNotNull(loginPasswordValue);
                                assertFalse(loginPasswordValue.isEmpty());
                            },
                            () -> {
                                String loginErrorInnerHTML = loginError.getAttribute("innerHTML");
                                assertNotNull(loginErrorInnerHTML);
                                assertFalse(loginErrorInnerHTML.isEmpty());
                            }
                        );
                    }
                );
            }
        );
    }

    private static Stream<Arguments> indexLogoutInteractionWithSession() {
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
    public void indexLogoutInteractionWithSession(WebBrowser webBrowser, String email, String password) throws Exception {
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
            }
        );
    }
}
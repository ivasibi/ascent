package org.ascent;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public abstract class ContainerEnvironment {

    public final String serverProtocol = "http://";

    public final String serverHostIP = "127.0.0.1";

    public final String serverContainerHostName = "host.docker.internal";

    public final static String serverHostPort = "8081";

    public final static String sessionCookieName = "AC-SESSION";

    public final static String sessionNamespace = "ascent";

    public final static String cacheKeyPrefix = "ascent:cache";

    public final static String cacheTTL = "3600000";

    public final static String loggingFilePath = "./logs";

    public final static String loggingFileName = "ascent-dev";

    public final static String loggingFileExtension = ".log";

    private final static String mySQLImage = "mysql:8.4.4";

    public static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(mySQLImage);

    private final static String redisImage = "redis:7.4.2";

    public final static String redisPassword = "ascent-dev";

    public static GenericContainer<?> redisContainer = new GenericContainer<>(redisImage)
        .withExposedPorts(6379)
        .withCommand("redis-server --requirepass " + redisPassword);

    public final VncRecordingMode vncRecordingMode = VncRecordingMode.RECORD_FAILING;

    public final String recordingFilePathPrefix = "./src/test/results/e2es";

    public final String chromeImage = "selenium/standalone-chrome:4.25.0";

    public final String firefoxImage = "selenium/standalone-firefox:4.25.0";

    public final String edgeImage = "selenium/standalone-edge:4.25.0";

    static {
        Startables.deepStart(
            mySQLContainer,
            redisContainer
        ).join();
    }

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("server.port", () -> serverHostPort);
        dynamicPropertyRegistry.add("server.servlet.session.cookie.name", () -> sessionCookieName);
        dynamicPropertyRegistry.add("logging.level.root", () -> "info");
        dynamicPropertyRegistry.add("logging.pattern.correlation", () -> "[%X{AL-CORRELATION}] ");
        dynamicPropertyRegistry.add("spring.profiles.default", () -> "dev");
        dynamicPropertyRegistry.add("spring.profiles.active", () -> "dev");
        dynamicPropertyRegistry.add("spring.jpa.open-in-view", () -> "false");
        dynamicPropertyRegistry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        dynamicPropertyRegistry.add("spring.session.store-type", () -> "redis");
        dynamicPropertyRegistry.add("spring.session.redis.namespace", () -> sessionNamespace);
        dynamicPropertyRegistry.add("spring.cache.type", () -> "redis");
        dynamicPropertyRegistry.add("spring.cache.redis.key-prefix", () -> cacheKeyPrefix);
        dynamicPropertyRegistry.add("spring.cache.redis.time-to-live", () -> cacheTTL);

        dynamicPropertyRegistry.add("logging.level.org.ascent", () -> "debug");
        dynamicPropertyRegistry.add("logging.file.name", () -> loggingFilePath + "/" + loggingFileName + loggingFileExtension);
        dynamicPropertyRegistry.add("logging.logback.rollingpolicy.max-file-size", () -> "5MB");
        dynamicPropertyRegistry.add("logging.logback.rollingpolicy.max-history", () -> "7");
        dynamicPropertyRegistry.add("logging.logback.rollingpolicy.file-name-pattern", () -> loggingFilePath + "/" + loggingFileName + ".%d{yyyy-MM-dd}.%i" + loggingFileExtension + ".zip");
        dynamicPropertyRegistry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        dynamicPropertyRegistry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        dynamicPropertyRegistry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        dynamicPropertyRegistry.add("spring.data.redis.host", () -> redisContainer.getHost());
        dynamicPropertyRegistry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        dynamicPropertyRegistry.add("spring.data.redis.password", () -> redisPassword);
    }
}
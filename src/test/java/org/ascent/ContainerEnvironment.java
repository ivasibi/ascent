package org.ascent;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public abstract class ContainerEnvironment {

    public final static String serverProtocol = "http://";

    public final static String serverIP = "localhost";

    public final static String serverPort = "8081";

    public final static String sessionCookieName = "AC-SESSION";

    private final static String mySQLImage = "mysql:8";

    public static MySQLContainer<?> mySQLContainer = new MySQLContainer<>(mySQLImage);

    private final static String redisImage = "redis:7.2.3";

    public final static String redisPassword = "test";

    public static GenericContainer<?> redisContainer = new GenericContainer<>(redisImage)
            .withExposedPorts(6379)
            .withCommand("redis-server --requirepass " + redisPassword);

    static {
        mySQLContainer.start();
        redisContainer.start();
    }

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("server.port", () -> serverPort);
        dynamicPropertyRegistry.add("server.servlet.session.cookie.name", () -> sessionCookieName);

        dynamicPropertyRegistry.add("spring.jpa.open-in-view", () -> "false");
        dynamicPropertyRegistry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        dynamicPropertyRegistry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        dynamicPropertyRegistry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        dynamicPropertyRegistry.add("spring.datasource.password", () -> mySQLContainer.getPassword());

        dynamicPropertyRegistry.add("spring.session.store-type", () -> "redis");
        dynamicPropertyRegistry.add("spring.data.redis.host", () -> redisContainer.getHost());
        dynamicPropertyRegistry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        dynamicPropertyRegistry.add("spring.data.redis.password", () -> redisPassword);
    }
}
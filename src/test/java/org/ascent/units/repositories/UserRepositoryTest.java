package org.ascent.units.repositories;

import jakarta.persistence.*;
import org.ascent.ContainerEnvironment;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.repositories.UserRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class UserRepositoryTest extends ContainerEnvironment {

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
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername("username");
        user.setEmail("username@email.com");
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        user.setDisabled(true);
        user.setRole(Role.USER);
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(Instant.now());

        User user2 = new User();
        user2.setUsername("username2");
        user2.setEmail("username2@email.com");
        user2.setPassword(bCryptPasswordEncoder.encode("password2"));
        user2.setDisabled(false);
        user2.setRole(Role.EDITOR);
        user2.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user2.setLastLogin(Instant.now());

        User user3 = new User();
        user3.setUsername("username3");
        user3.setEmail("username3@email.com");
        user3.setPassword(bCryptPasswordEncoder.encode("password3"));
        user3.setDisabled(true);
        user3.setRole(Role.MODERATOR);
        user3.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user3.setLastLogin(Instant.now());

        User user4 = new User();
        user4.setUsername("username4");
        user4.setEmail("username4@email.com");
        user4.setPassword(bCryptPasswordEncoder.encode("password4"));
        user4.setDisabled(false);
        user4.setRole(Role.ADMIN);
        user4.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user4.setLastLogin(Instant.now());

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

    private static Stream<String> checkIfSavedUserIsPersistedAndCachedByEmail() {
        return Stream.of("username@email.com", "username2@email.com", "username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserIsPersistedAndCachedByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(persistenceUser.getId(), cacheUser.getId()),
                            () -> assertEquals(persistenceUser.getUsername(), cacheUser.getUsername()),
                            () -> assertEquals(persistenceUser.getEmail(), cacheUser.getEmail()),
                            () -> assertEquals(persistenceUser.getPassword(), cacheUser.getPassword()),
                            () -> assertEquals(persistenceUser.isDisabled(), cacheUser.isDisabled()),
                            () -> assertEquals(persistenceUser.getRole(), cacheUser.getRole()),
                            () -> {
                                Instant persistenceUserCreatedOn = persistenceUser.getCreatedOn().truncatedTo(ChronoUnit.SECONDS);
                                Instant cacheUserCreatedOn = cacheUser.getCreatedOn().truncatedTo(ChronoUnit.SECONDS);
                                assertEquals(0, persistenceUserCreatedOn.compareTo(cacheUserCreatedOn));
                            },
                            () -> {
                                Instant persistenceUserLastLogin = persistenceUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                                Instant cacheUserLastLogin = cacheUser.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                                assertEquals(0, persistenceUserLastLogin.compareTo(cacheUserLastLogin));
                            }
                    );
                }
        );
    }

    private static Stream<String> checkIfNotSavedUserIsPersistedAndCachedByEmail() {
        return Stream.of("username5@email.com", "username6@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserIsPersistedAndCachedByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> assertThrows(NoResultException.class,
                        () -> query.getSingleResult()),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNull(cacheUser);
                }
        );
    }

    private static Stream<Arguments> checkIfSavingUserPersistsItAndUpdatesEmailCache() {
        return Stream.of(
                arguments("username5", "username5@email.com", "password5", true, Role.USER),
                arguments("username6", "username6@email.com", "password6", false, Role.EDITOR),
                arguments("username7", "username7@email.com", "password7", true, Role.MODERATOR),
                arguments("username8", "username8@email.com", "password8", false, Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavingUserPersistsItAndUpdatesEmailCache(String username, String email, String password, boolean disabled, Role role) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setDisabled(disabled);
        user.setRole(role);
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(Instant.now());

        userRepository.save(user);
        userRepository.flush();

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    assertAll(
                            () -> assertEquals(disabled, persistenceUser.isDisabled())
                    );
                },
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(disabled, cacheUser.isDisabled())
                    );
                }
        );

        user.setDisabled(!user.isDisabled());

        userRepository.save(user);
        userRepository.flush();

        entityManager.clear();

        assertAll(
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    assertAll(
                            () -> assertEquals(!disabled, persistenceUser.isDisabled())
                    );
                },
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(!disabled, cacheUser.isDisabled())
                    );
                }
        );
    }

    private static Stream<Arguments> checkIfSavingUserRestoresEmailCacheTTL() {
        return Stream.of(
                arguments("username5", "username5@email.com", "password5", true, Role.USER),
                arguments("username6", "username6@email.com", "password6", false, Role.EDITOR),
                arguments("username7", "username7@email.com", "password7", true, Role.MODERATOR),
                arguments("username8", "username8@email.com", "password8", false, Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavingUserRestoresEmailCacheTTL(String username, String email, String password, boolean disabled, Role role) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setDisabled(disabled);
        user.setRole(role);
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(Instant.now());

        userRepository.save(user);
        userRepository.flush();

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= ((Integer.parseInt(cacheTTL) / 1000) - 10)),
                            () -> assertTrue(cacheKeyTTL.intValue() <= (Integer.parseInt(cacheTTL) / 1000))
                    );
                }
        );

        redisTemplate.expire(cacheKey, 10, TimeUnit.SECONDS);

        assertAll(
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= 0),
                            () -> assertTrue(cacheKeyTTL.intValue() <= 10)
                    );
                }
        );

        user.setDisabled(!user.isDisabled());

        userRepository.save(user);
        userRepository.flush();

        assertAll(
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= ((Integer.parseInt(cacheTTL) / 1000) - 10)),
                            () -> assertTrue(cacheKeyTTL.intValue() <= (Integer.parseInt(cacheTTL) / 1000))
                    );
                }
        );
    }

    private static Stream<Arguments> checkIfDeletingUserRemovesItAndUpdatesEmailCache() {
        return Stream.of(
                arguments("username5", "username5@email.com", "password5", true, Role.USER),
                arguments("username6", "username6@email.com", "password6", false, Role.EDITOR),
                arguments("username7", "username7@email.com", "password7", true, Role.MODERATOR),
                arguments("username8", "username8@email.com", "password8", false, Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfDeletingUserRemovesItAndUpdatesEmailCache(String username, String email, String password, boolean disabled, Role role) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setDisabled(disabled);
        user.setRole(role);
        user.setCreatedOn(Instant.now().minus(1, ChronoUnit.HOURS));
        user.setLastLogin(Instant.now());

        userRepository.save(user);
        userRepository.flush();

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                },
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                }
        );

        userRepository.delete(user);
        userRepository.flush();

        entityManager.clear();

        assertAll(
                () -> assertThrows(NoResultException.class,
                        () -> query.getSingleResult()),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNull(cacheUser);
                }
        );
    }

    private static Stream<String> checkIfSavedUserExistsByUsername() {
        return Stream.of("username", "username2", "username3", "username4");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserExistsByUsername(String username) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertTrue(userRepository.existsByUsername(username));
    }

    private static Stream<String> checkIfNotSavedUserExistsByUsername() {
        return Stream.of("username5", "username6");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserExistsByUsername(String username) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertFalse(userRepository.existsByUsername(username));
    }

    private static Stream<String> checkIfSavedUserExistsByEmail() {
        return Stream.of("username@email.com", "username2@email.com", "username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserExistsByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertTrue(userRepository.existsByEmail(email));
    }

    private static Stream<String> checkIfNotSavedUserExistsByEmail() {
        return Stream.of("username5@email.com", "username6@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserExistsByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());

        assertFalse(userRepository.existsByEmail(email));
    }

    private static Stream<Arguments> checkIfSavedUserIsReturnedByFindByEmail() {
        return Stream.of(
                arguments("username", "username@email.com", "password", true, Role.USER),
                arguments("username2", "username2@email.com", "password2", false, Role.EDITOR),
                arguments("username3", "username3@email.com", "password3", true, Role.MODERATOR),
                arguments("username4", "username4@email.com", "password4", false, Role.ADMIN)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfSavedUserIsReturnedByFindByEmail(String username, String email, String password, boolean disabled, Role role) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        assertAll(
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
                            () -> assertEquals(disabled, user.isDisabled()),
                            () -> assertEquals(role, user.getRole()),
                            () -> assertNotNull(user.getCreatedOn()),
                            () -> assertNotNull(user.getLastLogin()),
                            () -> assertTrue(user.getCreatedOn().isBefore(user.getLastLogin()))
                    );
                }
        );
    }

    private static Stream<String> checkIfNotSavedUserIsReturnedByFindByEmail() {
        return Stream.of("username5@email.com", "username6@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotSavedUserIsReturnedByFindByEmail(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        assertNull(userRepository.findByEmail(email));
    }

    private static Stream<String> checkIfFindByEmailPopulatesEmailCache() {
        return Stream.of("username@email.com", "username2@email.com", "username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfFindByEmailPopulatesEmailCache(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        Set<String> redisKeys = redisTemplate.keys("*");
        if (redisKeys != null) {
            redisTemplate.delete(redisKeys);
        }

        User user = userRepository.findByEmail(email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> assertNotNull(user),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(user.getId(), cacheUser.getId()),
                            () -> assertEquals(user.getUsername(), cacheUser.getUsername()),
                            () -> assertEquals(user.getEmail(), cacheUser.getEmail()),
                            () -> assertEquals(user.getPassword(), cacheUser.getPassword()),
                            () -> assertEquals(user.isDisabled(), cacheUser.isDisabled()),
                            () -> assertEquals(user.getRole(), cacheUser.getRole()),
                            () -> {
                                Instant userCreatedOn = user.getCreatedOn().truncatedTo(ChronoUnit.SECONDS);
                                Instant cacheUserCreatedOn = cacheUser.getCreatedOn().truncatedTo(ChronoUnit.SECONDS);
                                assertEquals(0, userCreatedOn.compareTo(cacheUserCreatedOn));
                            },
                            () -> {
                                Instant userLastLogin = user.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                                Instant cacheUserLastLogin = user.getLastLogin().truncatedTo(ChronoUnit.SECONDS);
                                assertEquals(0, userLastLogin.compareTo(cacheUserLastLogin));
                            }
                    );
                }
        );
    }

    private static Stream<String> checkIfNotFindByEmailPopulatesEmailCache() {
        return Stream.of("username5@email.com", "username6@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfNotFindByEmailPopulatesEmailCache(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        Set<String> redisKeys = redisTemplate.keys("*");
        if (redisKeys != null) {
            redisTemplate.delete(redisKeys);
        }

        User user = userRepository.findByEmail(email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> assertNull(user),
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNull(cacheUser);
                }
        );
    }

    private static Stream<Arguments> checkIfFindByEmailRetrievesSavedUserFromEmailCache() {
        return Stream.of(
                arguments("username@email.com", true),
                arguments("username2@email.com", false),
                arguments("username3@email.com", true),
                arguments("username4@email.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfFindByEmailRetrievesSavedUserFromEmailCache(String email, boolean disabled) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        TypedQuery<User> query = entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    assertAll(
                            () -> assertEquals(disabled, persistenceUser.isDisabled())
                    );
                },
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(disabled, cacheUser.isDisabled())
                    );
                }
        );

        entityTransaction.begin();

        Query updateQuery = entityManager.createQuery("UPDATE User u SET u.disabled = :disabled WHERE u.email = :email");
        updateQuery.setParameter("disabled", !disabled);
        updateQuery.setParameter("email", email);
        updateQuery.executeUpdate();

        entityTransaction.commit();
        entityManager.clear();

        assertAll(
                () -> {
                    User user = userRepository.findByEmail(email);
                    assertNotNull(user);
                    assertAll(
                            () -> assertEquals(disabled, user.isDisabled())
                    );
                },
                () -> {
                    User persistenceUser = query.getSingleResult();
                    assertNotNull(persistenceUser);
                    assertAll(
                            () -> assertEquals(!disabled, persistenceUser.isDisabled())
                    );
                },
                () -> {
                    Object cacheObject = redisTemplate.opsForValue().get(cacheKey);
                    User cacheUser = (User) cacheObject;
                    assertNotNull(cacheUser);
                    assertAll(
                            () -> assertEquals(disabled, cacheUser.isDisabled())
                    );
                }
        );
    }

    private static Stream<String> checkIfFindByEmailDoesNotRestoreEmailCacheTTL() {
        return Stream.of("username@email.com", "username2@email.com", "username3@email.com", "username4@email.com");
    }

    @ParameterizedTest
    @MethodSource
    public void checkIfFindByEmailDoesNotRestoreEmailCacheTTL(String email) {
        assumeTrue(mySQLContainer.isCreated());
        assumeTrue(mySQLContainer.isRunning());
        assumeTrue(redisContainer.isCreated());
        assumeTrue(redisContainer.isRunning());

        String cacheKey = cacheKeyPrefix + ":users:email::" + email;

        assertAll(
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= ((Integer.parseInt(cacheTTL) / 1000) - 10)),
                            () -> assertTrue(cacheKeyTTL.intValue() <= (Integer.parseInt(cacheTTL) / 1000))
                    );
                }
        );

        redisTemplate.expire(cacheKey, 10, TimeUnit.SECONDS);

        assertAll(
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= 0),
                            () -> assertTrue(cacheKeyTTL.intValue() <= 10)
                    );
                }
        );

        User user = userRepository.findByEmail(email);

        assertAll(
                () -> assertNotNull(user),
                () -> {
                    Long cacheKeyTTL = redisTemplate.getExpire(cacheKey);
                    assertNotNull(cacheKeyTTL);
                    assertAll(
                            () -> assertTrue(cacheKeyTTL.intValue() >= 0),
                            () -> assertTrue(cacheKeyTTL.intValue() <= 10)
                    );
                }
        );
    }
}
package org.ascent.managers;

import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegisterManagerTest {

    @InjectMocks
    private RegisterManager registerManager;

    @Mock
    private UserRepository mockUserRepository;

    @ParameterizedTest
    @CsvSource({
            "username, username2@email.com, password",
            "username, username2@email.com, password2",
            "username, username3@email.com, password2",
            "username, username3@email.com, password3"
    })
    public void requestWithUsedUsernameThrowsUsernameAlreadyInUseException(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        when(mockUserRepository.existsByUsername(any())).thenReturn(true);

        assertThrows(UsernameAlreadyInUseException.class,
                () -> registerManager.register(registerRequest));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2",
            "username2, username2@email.com, password3",
            "username2, username3@email.com, password3",
            "username3, username3@email.com, password3"
    })
    public void requestWithUnusedUsernameDoesNotThrowException(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        when(mockUserRepository.existsByUsername(any())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(registerRequest));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username@email.com, password",
            "username2, username@email.com, password2",
            "username3, username@email.com, password2",
            "username3, username@email.com, password3"
    })
    public void requestWithUsedEmailThrowsEmailAlreadyInUseException(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        when(mockUserRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class,
                () -> registerManager.register(registerRequest));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2",
            "username2, username2@email.com, password3",
            "username2, username3@email.com, password3",
            "username3, username3@email.com, password3"
    })
    public void requestWithUnusedEmailDoesNotThrowException(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        when(mockUserRepository.existsByEmail(any())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(registerRequest));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2",
            "username2, username2@email.com, password3",
            "username2, username3@email.com, password3",
            "username3, username3@email.com, password3"
    })
    public void requestWithCreatedUserSavesUser(String username, String email, String password) {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        registerManager.register(registerRequest);

        verify(mockUserRepository, times(1)).save(any());
    }
}
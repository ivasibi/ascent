package org.ascent.managers;

import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.Test;
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

    @Test
    public void requestWithExistingUsernameThrowsUsernameAlreadyInUseException() {
        RegisterRequest registerRequest = new RegisterRequest();

        when(mockUserRepository.existsByUsername(any())).thenReturn(true);

        assertThrows(UsernameAlreadyInUseException.class,
                () -> registerManager.register(registerRequest));
    }

    @Test
    public void requestWithNonExistingUsernameDoesNotThrowException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPassword("password");

        when(mockUserRepository.existsByUsername(any())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(registerRequest));
    }

    @Test
    public void requestWithExistingEmailThrowsEmailAlreadyInUseException() {
        RegisterRequest registerRequest = new RegisterRequest();

        when(mockUserRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class,
                () -> registerManager.register(registerRequest));
    }

    @Test
    public void requestWithNonExistingEmailDoesNotThrowException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPassword("password");

        when(mockUserRepository.existsByEmail(any())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(registerRequest));
    }

    @Test
    public void requestWithNullPasswordThrowsIllegalArgumentException() {
        RegisterRequest registerRequest = new RegisterRequest();

        assertThrows(IllegalArgumentException.class,
                () -> registerManager.register(registerRequest));
    }

    @Test
    public void requestWithoutExceptionThrownSavesUser() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setPassword("password");

        registerManager.register(registerRequest);

        verify(mockUserRepository, times(1)).save(any());
    }
}
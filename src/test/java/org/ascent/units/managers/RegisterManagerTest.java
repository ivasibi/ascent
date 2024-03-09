package org.ascent.units.managers;

import org.ascent.entities.User;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.managers.RegisterManager;
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
        RegisterRequest mockRegisterRequest = mock();

        when(mockUserRepository.existsByUsername(any())).thenReturn(true);

        assertThrows(UsernameAlreadyInUseException.class,
                () -> registerManager.register(mockRegisterRequest));
    }

    @Test
    public void requestWithNonExistingUsernameDoesNotThrowException() {
        RegisterRequest mockRegisterRequest = mock();
        when(mockRegisterRequest.getPassword()).thenReturn("password");

        when(mockUserRepository.existsByUsername(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(mockRegisterRequest));
    }

    @Test
    public void requestWithExistingEmailThrowsEmailAlreadyInUseException() {
        RegisterRequest mockRegisterRequest = mock();

        when(mockUserRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(EmailAlreadyInUseException.class,
                () -> registerManager.register(mockRegisterRequest));
    }

    @Test
    public void requestWithNonExistingEmailDoesNotThrowException() {
        RegisterRequest mockRegisterRequest = mock();
        when(mockRegisterRequest.getPassword()).thenReturn("password");

        when(mockUserRepository.existsByEmail(anyString())).thenReturn(false);

        assertDoesNotThrow(() -> registerManager.register(mockRegisterRequest));
    }

    @Test
    public void requestWithoutPasswordThrowsIllegalArgumentException() {
        RegisterRequest mockRegisterRequest = mock();

        assertThrows(IllegalArgumentException.class,
                () -> registerManager.register(mockRegisterRequest));
    }

    @Test
    public void requestWithoutExceptionThrownSavesUser() {
        RegisterRequest mockRegisterRequest = mock();
        when(mockRegisterRequest.getPassword()).thenReturn("password");

        registerManager.register(mockRegisterRequest);

        verify(mockUserRepository, times(1)).save(any(User.class));
    }
}
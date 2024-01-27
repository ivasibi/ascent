package org.ascent.managers;

import jakarta.servlet.http.HttpServletRequest;
import org.ascent.entities.User;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LoginManagerTest {

    @InjectMocks
    private LoginManager loginManager;

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    private HttpServletRequest mockHttpServletRequest;

    @Test
    public void requestWithNonExistingUserThrowsInvalidCredentialsException() {
        LoginRequest loginRequest = new LoginRequest();

        when(mockUserRepository.findByEmail(any())).thenReturn(null);

        assertThrows(InvalidCredentialsException.class,
                () -> loginManager.login(any(), loginRequest));
    }

    @Test
    public void requestWithDisabledUserThrowsUserDisabledException() {
        LoginRequest loginRequest = new LoginRequest();

        User user = new User();
        user.setDisabled(true);
        when(mockUserRepository.findByEmail(any())).thenReturn(user);

        assertThrows(UserDisabledException.class,
                () -> loginManager.login(any(), loginRequest));
    }

    @Test
    public void requestWithNonMatchingPasswordThrowsInvalidCredentialsException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setPassword(bCryptPasswordEncoder.encode("password2"));
        when(mockUserRepository.findByEmail(any())).thenReturn(user);

        assertThrows(InvalidCredentialsException.class,
                () -> loginManager.login(any(), loginRequest));
    }

    @Test
    public void requestWithSameSessionThrowsIllegalStateException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        when(mockUserRepository.findByEmail(any())).thenReturn(user);

        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession());

        assertThrows(IllegalStateException.class,
                () -> loginManager.login(mockHttpServletRequest, loginRequest));
    }

    @Test
    public void requestWithoutExceptionThrownSetsSessionAndUpdatesUser() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPassword("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setPassword(bCryptPasswordEncoder.encode("password"));
        when(mockUserRepository.findByEmail(any())).thenReturn(user);

        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        loginManager.login(mockHttpServletRequest, loginRequest);

        verify(mockHttpServletRequest, times(2)).getSession(anyBoolean());
        verify(mockUserRepository, times(1)).save(any());
    }
}
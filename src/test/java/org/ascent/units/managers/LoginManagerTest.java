package org.ascent.units.managers;

import jakarta.servlet.http.HttpServletRequest;
import org.ascent.entities.User;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.managers.LoginManager;
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

    @Test
    public void requestWithNonExistingUserThrowsInvalidCredentialsException() {
        HttpServletRequest mockHttpServletRequest = mock();

        LoginRequest mockLoginRequest = mock();

        when(mockUserRepository.findByEmail(any())).thenReturn(null);

        assertThrows(InvalidCredentialsException.class,
                () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    @Test
    public void requestWithExistingUserDoesNotThrowException() {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.isDisabled()).thenReturn(false);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertDoesNotThrow(() -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    @Test
    public void requestWithDisabledUserThrowsUserDisabledException() {
        HttpServletRequest mockHttpServletRequest = mock();

        LoginRequest mockLoginRequest = mock();

        User mockUser = mock();
        when(mockUser.isDisabled()).thenReturn(true);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertThrows(UserDisabledException.class,
                () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    @Test
    public void requestWithNonMatchingPasswordThrowsInvalidCredentialsException() {
        HttpServletRequest mockHttpServletRequest = mock();

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password2"));

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertThrows(InvalidCredentialsException.class,
                () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    @Test
    public void requestWithSameSessionThrowsIllegalStateException() {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertThrows(IllegalStateException.class,
                () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    @Test
    public void requestWithoutExceptionThrownInvalidatesOldSession() {
        HttpServletRequest mockHttpServletRequest = mock();
        MockHttpSession mockHttpSession = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(mockHttpSession, new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockHttpServletRequest, times(1)).getSession(false);
        verify(mockHttpSession, times(1)).invalidate();
    }

    @Test
    public void requestWithoutExceptionThrownCreatesNewSession() {
        HttpServletRequest mockHttpServletRequest = mock();
        MockHttpSession mockHttpSession = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), mockHttpSession);

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockHttpServletRequest, times(1)).getSession(true);
        verify(mockHttpSession, times(1)).setMaxInactiveInterval(anyInt());
        verify(mockHttpSession, times(3)).setAttribute(anyString(), any());
    }

    @Test
    public void requestWithoutExceptionThrownUpdatesUser() {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockUser, times(1)).setLastLogin(any());
        verify(mockUserRepository, times(1)).save(any(User.class));
    }
}
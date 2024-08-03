package org.ascent.units.managers;

import jakarta.servlet.http.HttpServletRequest;
import org.ascent.entities.User;
import org.ascent.enums.Role;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.managers.LoginManager;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

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

    private static Stream<Role> requestWithExistingUserDoesNotThrowException() {
        return Stream.of(Role.USER, Role.EDITOR, Role.MODERATOR, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void requestWithExistingUserDoesNotThrowException(Role role) {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(role);

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
    public void requestWithoutRoleThrowsNullPointerException() {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(null);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertThrows(NullPointerException.class,
            () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    private static Stream<Role> requestWithSameSessionThrowsIllegalStateException() {
        return Stream.of(Role.USER, Role.EDITOR, Role.MODERATOR, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void requestWithSameSessionThrowsIllegalStateException(Role role) {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(role);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        assertThrows(IllegalStateException.class,
            () -> loginManager.login(mockHttpServletRequest, mockLoginRequest));
    }

    private static Stream<Role> requestWithoutExceptionThrownInvalidatesOldSession() {
        return Stream.of(Role.USER, Role.EDITOR, Role.MODERATOR, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void requestWithoutExceptionThrownInvalidatesOldSession(Role role) {
        HttpServletRequest mockHttpServletRequest = mock();
        MockHttpSession mockHttpSession = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(mockHttpSession, new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(role);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockHttpServletRequest, times(1)).getSession(false);
        verify(mockHttpSession, times(1)).invalidate();
    }

    private static Stream<Arguments> requestWithoutExceptionThrownCreatesNewSession() {
        return Stream.of(
            arguments(Role.USER, 120 * 60),
            arguments(Role.EDITOR, 60 * 60),
            arguments(Role.MODERATOR, 30 * 60),
            arguments(Role.ADMIN, 15 * 60)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void requestWithoutExceptionThrownCreatesNewSession(Role role, int maxInactiveInterval) {
        HttpServletRequest mockHttpServletRequest = mock();
        MockHttpSession mockHttpSession = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), mockHttpSession);

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(role);
        when(mockUser.getUsername()).thenReturn("username");

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockHttpServletRequest, times(1)).getSession(true);
        verify(mockHttpSession, times(1)).setMaxInactiveInterval(maxInactiveInterval);
        verify(mockHttpSession, times(3)).setAttribute(anyString(), any());
        verify(mockHttpSession, times(1)).setAttribute("logged", true);
        verify(mockHttpSession, times(1)).setAttribute("username", "username");
        verify(mockHttpSession, times(1)).setAttribute("role", role);
    }

    private static Stream<Role> requestWithoutExceptionThrownUpdatesUser() {
        return Stream.of(Role.USER, Role.EDITOR, Role.MODERATOR, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void requestWithoutExceptionThrownUpdatesUser(Role role) {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(new MockHttpSession(), new MockHttpSession());

        LoginRequest mockLoginRequest = mock();
        when(mockLoginRequest.getPassword()).thenReturn("password");

        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

        User mockUser = mock();
        when(mockUser.getPassword()).thenReturn(bCryptPasswordEncoder.encode("password"));
        when(mockUser.getRole()).thenReturn(role);

        when(mockUserRepository.findByEmail(any())).thenReturn(mockUser);

        loginManager.login(mockHttpServletRequest, mockLoginRequest);

        verify(mockUser, times(1)).setLastLogin(any());
        verify(mockUserRepository, times(1)).save(any(User.class));
    }
}
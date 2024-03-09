package org.ascent.units.managers;

import org.ascent.enums.Role;
import org.ascent.managers.ViewManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.ModelAndView;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@SpringBootTest
public class ViewManagerTest {

    @Autowired
    private ViewManager viewManager;

    private static Stream<Role> requestNavbarWithLoggedUserSetsModelAndView() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void requestNavbarWithLoggedUserSetsModelAndView(Role role) {
        MockHttpSession mockHttpSession = mock();
        when(mockHttpSession.getAttribute("logged")).thenReturn(true);
        when(mockHttpSession.getAttribute("username")).thenReturn("username");
        when(mockHttpSession.getAttribute("role")).thenReturn(role);

        ModelAndView mockModelAndView = mock();

        viewManager.navbar(mockHttpSession, mockModelAndView);

        verify(mockHttpSession, times(1)).getAttribute("logged");
        verify(mockHttpSession, times(1)).getAttribute("username");
        verify(mockHttpSession, times(1)).getAttribute("role");

        verify(mockModelAndView, times(1)).addObject("logged", true);
        verify(mockModelAndView, times(1)).addObject("username", "username");
        verify(mockModelAndView, times(1)).addObject("role", role);
    }

    @Test
    public void requestNavbarWithoutLoggedUserSetsModelAndView() {
        MockHttpSession mockHttpSession = mock();
        when(mockHttpSession.getAttribute("logged")).thenReturn(false);

        ModelAndView mockModelAndView = mock();

        viewManager.navbar(mockHttpSession, mockModelAndView);

        verify(mockHttpSession, times(1)).getAttribute("logged");

        verify(mockModelAndView, times(1)).addObject("logged", false);
    }

    @Test
    public void requestNavbarWithoutSessionSetsModelAndView() {
        MockHttpSession mockHttpSession = mock();
        when(mockHttpSession.getAttribute("logged")).thenReturn(null);

        ModelAndView mockModelAndView = mock();

        viewManager.navbar(mockHttpSession, mockModelAndView);

        verify(mockHttpSession, times(1)).getAttribute("logged");

        verify(mockModelAndView, times(1)).addObject("logged", false);
    }
}
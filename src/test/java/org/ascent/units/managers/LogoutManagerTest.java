package org.ascent.units.managers;

import jakarta.servlet.http.HttpServletRequest;
import org.ascent.managers.LogoutManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LogoutManagerTest {

    @Autowired
    private LogoutManager logoutManager;

    @Test
    public void requestWithoutSessionDoesNotThrowException() {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(null);

        assertDoesNotThrow(() -> logoutManager.logout(mockHttpServletRequest));
    }

    @Test
    public void requestWithSessionInvalidatesSession() {
        HttpServletRequest mockHttpServletRequest = mock();

        MockHttpSession mockHttpSession = mock();
        when(mockHttpServletRequest.getSession(anyBoolean())).thenReturn(mockHttpSession);

        logoutManager.logout(mockHttpServletRequest);

        verify(mockHttpSession, times(1)).invalidate();
    }
}
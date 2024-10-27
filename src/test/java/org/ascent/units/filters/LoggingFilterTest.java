package org.ascent.units.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ascent.filters.LoggingFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LoggingFilterTest {

    @Autowired
    private LoggingFilter loggingFilter;

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    public void requestAnywhereLogsRequestDetailsOnConsole(CapturedOutput capturedOutput) throws Exception {
        HttpServletRequest mockHttpServletRequest = mock();
        when(mockHttpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(mockHttpServletRequest.getMethod()).thenReturn("GET");
        when(mockHttpServletRequest.getRequestURI()).thenReturn("/");

        HttpServletResponse mockHttpServletResponse = mock();
        when(mockHttpServletResponse.getStatus()).thenReturn(200);

        FilterChain mockFilterChain = mock();

        loggingFilter.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertAll(
            () -> assertTrue(capturedOutput.getOut().contains("INFO")),
            () -> assertTrue(capturedOutput.getOut().contains("ascent.filters.LoggingFilter")),
            () -> assertTrue(capturedOutput.getOut().contains("127.0.0.1")),
            () -> assertTrue(capturedOutput.getOut().contains("GET")),
            () -> assertTrue(capturedOutput.getOut().contains("/")),
            () -> assertTrue(capturedOutput.getOut().contains("200"))
        );
    }
}
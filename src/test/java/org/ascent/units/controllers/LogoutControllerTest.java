package org.ascent.units.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.ascent.controllers.LogoutController;
import org.ascent.managers.LogoutManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class LogoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private LogoutManager mockLogoutManager;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LogoutController(mockLogoutManager)).build();
    }

    @Test
    public void callWithoutHTMXHeaderReturnsNotFound() throws Exception {
        mockMvc.perform(
                        get("/logout"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void callWithPostHTTPMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(
                        post("/logout")
                                .header("HX-Request", "true"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void callWithoutExceptionThrownReturnsOkAndSuccess() throws Exception {
        mockMvc.perform(
                        get("/logout")
                                .header("HX-Request", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("responses/logout_response :: success"));
    }

    @Test
    public void callThenCallsLogoutManagerLogoutMethod() throws Exception {
        mockMvc.perform(
                        get("/logout")
                                .header("HX-Request", "true"))
                .andDo(print());

        verify(mockLogoutManager, times(1)).logout(any(HttpServletRequest.class));
    }

    @Test
    public void callWithRuntimeExceptionThrownReturnsInternalServerErrorAndError() throws Exception {
        doThrow(new RuntimeException()).when(mockLogoutManager).logout(any(HttpServletRequest.class));

        mockMvc.perform(
                        get("/logout")
                                .header("HX-Request", "true"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("responses/logout_response :: error"))
                .andExpect(result -> assertNotNull(result.getResolvedException()));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    public void callWithRuntimeExceptionThrownLogsError(CapturedOutput capturedOutput) throws Exception {
        doThrow(new RuntimeException("RuntimeException")).when(mockLogoutManager).logout(any(HttpServletRequest.class));

        mockMvc.perform(
                        get("/logout")
                                .header("HX-Request", "true"))
                .andDo(print());

        assertAll(
                () -> assertTrue(capturedOutput.getOut().contains("ERROR")),
                () -> assertTrue(capturedOutput.getOut().contains("ascent.controllers.LogoutController")),
                () -> assertTrue(capturedOutput.getOut().contains("RuntimeException"))
        );
    }
}
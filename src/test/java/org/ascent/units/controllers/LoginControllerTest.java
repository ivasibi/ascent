package org.ascent.units.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.ascent.controllers.LoginController;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.managers.LoginManager;
import org.ascent.requests.LoginRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private LoginManager mockLoginManager;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(new LoginController(mockLoginManager)).build();
    }

    @Test
    public void callWithoutHTMXHeaderReturnsNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void callWithGetHTTPMethodReturnsMethodNotAllowed() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        get("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void callWithUrlEncodedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        String loginRequestString = loginRequest.toString();
        String loginRequestUrlEncoded = URLEncoder.encode(loginRequestString, StandardCharsets.UTF_8);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .content(loginRequestUrlEncoded))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void callWithoutExceptionThrownReturnsOkAndSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("responses/login_response :: success"));
    }

    @Test
    public void callThenCallsLoginManagerLoginMethod() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print());

        verify(mockLoginManager, times(1)).login(any(HttpServletRequest.class), any(LoginRequest.class));
    }

    @Test
    public void callWithInvalidCredentialsExceptionThrownReturnsUnauthorizedAndInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        doThrow(new InvalidCredentialsException()).when(mockLoginManager).login(any(HttpServletRequest.class), any(LoginRequest.class));

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(view().name("responses/login_response :: invalid_credentials"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidCredentialsException));
    }

    @Test
    public void callWithUserDisabledExceptionThrownReturnsUnauthorizedAndUserDisabled() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        doThrow(new UserDisabledException()).when(mockLoginManager).login(any(HttpServletRequest.class), any(LoginRequest.class));

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(view().name("responses/login_response :: user_disabled"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UserDisabledException));
    }

    @Test
    public void callWithRuntimeExceptionThrownReturnsInternalServerErrorAndError() throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        doThrow(new RuntimeException()).when(mockLoginManager).login(any(HttpServletRequest.class), any(LoginRequest.class));

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("responses/login_response :: error"))
                .andExpect(result -> assertNotNull(result.getResolvedException()));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    public void callWithRuntimeExceptionThrownLogsError(CapturedOutput capturedOutput) throws Exception {
        LoginRequest loginRequest = new LoginRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String loginRequestJson = objectMapper.writeValueAsString(loginRequest);

        doThrow(new RuntimeException("RuntimeException")).when(mockLoginManager).login(any(HttpServletRequest.class), any(LoginRequest.class));

        mockMvc.perform(
                        post("/login")
                                .header("HX-Request", "true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequestJson))
                .andDo(print());

        assertAll(
                () -> assertTrue(capturedOutput.getOut().contains("ERROR")),
                () -> assertTrue(capturedOutput.getOut().contains("ascent.controllers.LoginController")),
                () -> assertTrue(capturedOutput.getOut().contains("RuntimeException"))
        );
    }
}
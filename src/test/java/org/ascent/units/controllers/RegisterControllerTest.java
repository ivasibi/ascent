package org.ascent.units.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.controllers.RegisterController;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.managers.RegisterManager;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
public class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private RegisterManager mockRegisterManager;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RegisterController(mockRegisterManager)).build();
    }

    @Test
    public void callWithoutHTMXHeaderReturnsNotFound() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void callWithGetHTTPMethodReturnsMethodNotAllowed() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                get("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void callWithUrlEncodedMediaTypeReturnsUnsupportedMediaType() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        String registerRequestString = registerRequest.toString();
        String registerRequestUrlEncoded = URLEncoder.encode(registerRequestString, StandardCharsets.UTF_8);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content(registerRequestUrlEncoded))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    public void callWithoutExceptionThrownReturnsCreatedAndSuccess() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: success"));
    }

    @Test
    public void callWithUsernameAlreadyInUseExceptionThrownReturnsConflictAndUsernameAlreadyInUse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        doThrow(new UsernameAlreadyInUseException()).when(mockRegisterManager).register(any());

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: username_already_in_use"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UsernameAlreadyInUseException));
    }

    @Test
    public void callWithEmailAlreadyInUseExceptionThrownReturnsConflictAndEmailAlreadyInUse() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        doThrow(new EmailAlreadyInUseException()).when(mockRegisterManager).register(any());

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: email_already_in_use"))
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof EmailAlreadyInUseException));
    }

    @Test
    public void callWithRuntimeExceptionThrownReturnsInternalServerErrorAndError() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        doThrow(new RuntimeException()).when(mockRegisterManager).register(any());

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(model().size(0))
                .andExpect(view().name("responses/register_response :: error"))
                .andExpect(result -> assertNotNull(result.getResolvedException()));
    }
}
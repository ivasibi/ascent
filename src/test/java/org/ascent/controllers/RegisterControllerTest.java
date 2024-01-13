package org.ascent.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.managers.RegisterManager;
import org.ascent.requests.RegisterRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2"
    })
    public void callWithoutHTMXHeaderReturnsNotFound(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
            post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerRequestJson))
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2"
    })
    public void callWithUrlEncodedMediaTypeReturnsUnsupportedMediaType(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

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

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2",
            "username2, username2@email.com, password3",
            "username2, username3@email.com, password3",
            "username3, username3@email.com, password3"
    })
    public void callWithCreatedUserReturnsCreatedAndSuccess(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        ObjectMapper objectMapper = new ObjectMapper();
        String registerRequestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(
                post("/register")
                        .header("HX-Request", true)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(view().name("responses/register_response :: success"));
    }

    @ParameterizedTest
    @CsvSource({
            "username, username2@email.com, password",
            "username, username2@email.com, password2",
            "username, username3@email.com, password2",
            "username, username3@email.com, password3"
    })
    public void callWithUsedUsernameReturnsConflictAndUsernameAlreadyInUse(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

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
                .andExpect(view().name("responses/register_response :: username_already_in_use"))
                .andExpect(result -> Assertions.assertTrue(result.getResolvedException() instanceof UsernameAlreadyInUseException));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username@email.com, password",
            "username2, username@email.com, password2",
            "username3, username@email.com, password2",
            "username3, username@email.com, password3"
    })
    public void callWithUsedEmailReturnsConflictAndEmailAlreadyInUse(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

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
                .andExpect(view().name("responses/register_response :: email_already_in_use"))
                .andExpect(result -> Assertions.assertTrue(result.getResolvedException() instanceof EmailAlreadyInUseException));
    }

    @ParameterizedTest
    @CsvSource({
            "username2, username2@email.com, password2"
    })
    public void callWithRuntimeExceptionThrownReturnsInternalServerErrorAndError(String username, String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

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
                .andExpect(view().name("responses/register_response :: error"))
                .andExpect(result -> Assertions.assertNotNull(result.getResolvedException()));
    }
}
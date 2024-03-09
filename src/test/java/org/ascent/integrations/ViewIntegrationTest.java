package org.ascent.integrations;

import org.ascent.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class ViewIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static Stream<Role> callIndexWithLoggedUserReturnsOkAndIndex() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void callIndexWithLoggedUserReturnsOkAndIndex(Role role) throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", true);
        mockHttpSession.setAttribute("username", "username");
        mockHttpSession.setAttribute("role", role);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(3))
                .andExpect(model().attribute("logged", true))
                .andExpect(model().attribute("username", "username"))
                .andExpect(model().attribute("role", role))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>")));
    }

    @Test
    public void callIndexWithoutLoggedUserReturnsOkAndIndex() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", false);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    @Test
    public void callIndexWithoutSessionReturnsOkAndIndex() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", null);

        mockMvc.perform(
                        get("/")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("index"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    private static Stream<Role> callNavbarWithLoggedUserReturnsOkAndNavbar() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    @ParameterizedTest
    @MethodSource
    public void callNavbarWithLoggedUserReturnsOkAndNavbar(Role role) throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", true);
        mockHttpSession.setAttribute("username", "username");
        mockHttpSession.setAttribute("role", role);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(3))
                .andExpect(model().attribute("logged", true))
                .andExpect(model().attribute("username", "username"))
                .andExpect(model().attribute("role", role))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Logout</span>")));
    }

    @Test
    public void callNavbarWithoutLoggedUserReturnsOkAndNavbar() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", false);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }

    @Test
    public void callNavbarWithoutSessionReturnsOkAndNavbar() throws Exception {
        MockHttpSession mockHttpSession = new MockHttpSession();
        mockHttpSession.setAttribute("logged", null);

        mockMvc.perform(
                        get("/navbar")
                                .session(mockHttpSession))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(model().size(1))
                .andExpect(model().attribute("logged", false))
                .andExpect(view().name("fragments/navbar :: navbar"))
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Login</span>")))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("<span class=\"ms-1 d-none d-sm-inline\">Register</span>")));
    }
}
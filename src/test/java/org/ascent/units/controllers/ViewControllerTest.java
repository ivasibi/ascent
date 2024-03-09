package org.ascent.units.controllers;

import jakarta.servlet.http.HttpSession;
import org.ascent.controllers.ViewController;
import org.ascent.managers.ViewManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.ModelAndView;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureWebMvc
@AutoConfigureMockMvc
public class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ViewManager mockViewManager;

    @BeforeEach
    public void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ViewController(mockViewManager)).build();
    }

    @Test
    public void callIndexWithPostHTTPMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(
                        post("/"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void callIndexReturnsOkAndIndex() throws Exception {
        mockMvc.perform(
                        get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    public void callIndexThenCallsViewManagerNavbarMethod() throws Exception {
        mockMvc.perform(
                        get("/"))
                .andDo(print());

        verify(mockViewManager, times(1)).navbar(any(HttpSession.class), any(ModelAndView.class));
    }

    @Test
    public void callNavbarWithPostHTTPMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(
                        post("/navbar"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void callNavbarReturnsOkAndNavbar() throws Exception {
        mockMvc.perform(
                        get("/navbar"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/navbar :: navbar"));
    }

    @Test
    public void callNavbarThenCallsViewManagerNavbarMethod() throws Exception {
        mockMvc.perform(
                        get("/navbar"))
                .andDo(print());

        verify(mockViewManager, times(1)).navbar(any(HttpSession.class), any(ModelAndView.class));
    }
}
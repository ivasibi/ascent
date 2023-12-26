package org.ascent.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ascent.managers.ViewManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final ViewManager viewManager;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/")
    public ModelAndView index(HttpSession httpSession) {
        ModelAndView modelAndView = new ModelAndView("index");
        viewManager.navbar(httpSession, modelAndView);
        return modelAndView;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/navbar")
    public ModelAndView navbar(HttpSession httpSession) {
        ModelAndView modelAndView = new ModelAndView("fragments/navbar :: navbar");
        viewManager.navbar(httpSession, modelAndView);
        return modelAndView;
    }
}
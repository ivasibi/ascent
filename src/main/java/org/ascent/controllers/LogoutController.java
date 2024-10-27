package org.ascent.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ascent.managers.LogoutManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequiredArgsConstructor
public class LogoutController {

    private final static Logger logger = LoggerFactory.getLogger(LogoutController.class.getName());

    private final LogoutManager logoutManager;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/logout", headers = "HX-Request")
    public String logout(HttpServletRequest httpServletRequest) {
        logoutManager.logout(httpServletRequest);
        return "responses/logout_response :: success";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    private String handleException(Exception e) {
        logger.error("{} {}", e.getClass().getSimpleName(), e.getMessage());
        return "responses/logout_response :: error";
    }
}
package org.ascent.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.managers.LoginManager;
import org.ascent.requests.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.logging.Logger;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final static Logger logger = Logger.getLogger(LoginController.class.getName());

    private final LoginManager loginManager;

    @ResponseStatus(HttpStatus.OK)
    @PostMapping(value = "/login", headers = "HX-Request", consumes = "application/json")
    public String login(HttpServletRequest httpServletRequest, @RequestBody LoginRequest loginRequest) {
        loginManager.login(httpServletRequest, loginRequest);
        return "responses/login_response :: success";
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InvalidCredentialsException.class)
    private String handleInvalidCredentialsException() {
        return "responses/login_response :: invalid_credentials";
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UserDisabledException.class)
    private String handleUserDisabledException() {
        return "responses/login_response :: user_disabled";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    private String handleException(Exception e) {
        logger.severe(e.getMessage());
        return "responses/login_response :: error";
    }
}
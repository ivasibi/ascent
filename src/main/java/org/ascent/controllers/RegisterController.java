package org.ascent.controllers;

import lombok.RequiredArgsConstructor;
import org.ascent.exceptions.EmailAlreadyInUseException;
import org.ascent.exceptions.UsernameAlreadyInUseException;
import org.ascent.managers.RegisterManager;
import org.ascent.requests.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.logging.Logger;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final static Logger logger = Logger.getLogger(RegisterController.class.getName());

    private final RegisterManager registerManager;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "/register", headers = "HX-Request", consumes = "application/json")
    public String register(@RequestBody RegisterRequest registerRequest) {
        registerManager.register(registerRequest);
        return "responses/register_response :: success";
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UsernameAlreadyInUseException.class)
    private String handleUsernameAlreadyInUse() {
        return "responses/register_response :: username_already_in_use";
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EmailAlreadyInUseException.class)
    private String handleEmailAlreadyInUse() {
        return "responses/register_response :: email_already_in_use";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    private String handleException(Exception e) {
        logger.severe(e.getMessage());
        return "responses/register_response :: error";
    }
}
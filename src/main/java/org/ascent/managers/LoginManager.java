package org.ascent.managers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ascent.configurations.BCryptConfiguration;
import org.ascent.entities.User;
import org.ascent.exceptions.InvalidCredentialsException;
import org.ascent.exceptions.UserDisabledException;
import org.ascent.repositories.UserRepository;
import org.ascent.requests.LoginRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginManager {

    private final UserRepository userRepository;

    private final BCryptConfiguration bCryptConfiguration;

    @Transactional
    public void login(HttpServletRequest httpServletRequest, LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {
            throw new InvalidCredentialsException();
        }

        if (user.isDisabled()) {
            throw new UserDisabledException();
        }

        if (!bCryptConfiguration.bCryptPasswordEncoder().matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession != null) {
            httpSession.invalidate();
        }

        httpSession = httpServletRequest.getSession(true);

        httpSession.setMaxInactiveInterval(600);
        httpSession.setAttribute("logged", true);
        httpSession.setAttribute("username", user.getUsername());
        httpSession.setAttribute("role", user.getRole());

        user.setLastLogin(Instant.now());
        userRepository.save(user);
    }
}
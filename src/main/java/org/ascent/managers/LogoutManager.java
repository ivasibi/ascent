package org.ascent.managers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutManager {

    public void logout(HttpServletRequest httpServletRequest) {
        HttpSession httpSession = httpServletRequest.getSession(false);

        if (httpSession != null) {
            httpSession.invalidate();
        }
    }
}
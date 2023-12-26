package org.ascent.managers;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.ascent.enums.Role;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;

@Service
@RequiredArgsConstructor
public class ViewManager {

    public void navbar(HttpSession httpSession, ModelAndView modelAndView) {
        if (Boolean.TRUE.equals(httpSession.getAttribute("logged"))) {
            modelAndView.addObject("logged", true);

            String username = (String) httpSession.getAttribute("username");
            Role role = (Role) httpSession.getAttribute("role");

            modelAndView.addObject("username", username);
            modelAndView.addObject("role", role);
        } else {
            modelAndView.addObject("logged", false);
        }
    }
}
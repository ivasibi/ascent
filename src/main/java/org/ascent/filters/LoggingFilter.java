package org.ascent.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Order(1)
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(LoggingFilter.class.getName());

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        MDC.put("AL-CORRELATION", UUID.randomUUID().toString());

        logger.info("{} {} {}", httpServletRequest.getRemoteAddr(), httpServletRequest.getMethod(), httpServletRequest.getRequestURI());

        Instant start = Instant.now();
        filterChain.doFilter(httpServletRequest, httpServletResponse);
        Instant finish = Instant.now();

        logger.info("{} {}ms", httpServletResponse.getStatus(), start.until(finish, ChronoUnit.MILLIS));

        MDC.clear();
    }
}
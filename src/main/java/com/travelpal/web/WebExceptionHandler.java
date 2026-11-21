package com.travelpal.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Turns "not found" errors from the services (e.g. a plan id that doesn't exist) into a 404 page
 * instead of a 500 server error. "Not allowed" (AccessDeniedException) is already turned into 403 by Spring Security.
 */
@ControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public void notFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}

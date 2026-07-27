package com.hellodoctor.helios.security;

import com.hellodoctor.helios.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Produces JSON {@link ApiError} responses for unauthenticated (401) and unauthorized (403)
 * access instead of the default HTML error pages.
 */
@Component
public class RestAuthErrorHandlers {

    private final ObjectMapper objectMapper;

    public RestAuthErrorHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                write(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentication required.", request.getRequestURI());
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                write(response, HttpServletResponse.SC_FORBIDDEN,
                        "You do not have permission to perform this action.", request.getRequestURI());
    }

    private void write(HttpServletResponse response, int status, String message, String path)
            throws java.io.IOException {
        ApiError body = ApiError.of(status, message, message, path);
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

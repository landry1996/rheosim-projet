package com.rheosim.infrastructure.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@Order(3)
public class InputSanitizationFilter extends OncePerRequestFilter {

    private static final Pattern XSS_SCRIPT = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern XSS_EVENT = Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INJECTION = Pattern.compile("(\\b(UNION|SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|EXEC|EXECUTE)\\b\\s)", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PARAM_LENGTH = 10000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (isContentTypeJson(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest sanitizedRequest = new SanitizedRequestWrapper(request);
        filterChain.doFilter(sanitizedRequest, response);
    }

    private boolean isContentTypeJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    private static class SanitizedRequestWrapper extends HttpServletRequestWrapper {
        public SanitizedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        private String sanitize(String value) {
            if (value == null) return null;
            if (value.length() > MAX_PARAM_LENGTH) {
                value = value.substring(0, MAX_PARAM_LENGTH);
            }
            value = XSS_SCRIPT.matcher(value).replaceAll("");
            value = XSS_EVENT.matcher(value).replaceAll("");
            value = value.replace("<", "&lt;").replace(">", "&gt;");
            return value;
        }
    }
}

package com.community.security;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class JwtFilter extends AuthenticatingFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final List<String> WHITELIST_PREFIX = Arrays.asList("/api/auth/");
    private static final List<String> WHITELIST_PATH = Arrays.asList("/api/health", "/error", "/api/repair/file", "/api/services/file");

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String authHeader = ((HttpServletRequest) request).getHeader("Authorization");
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return new JwtToken(token);
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        HttpServletRequest req = (HttpServletRequest) request;
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            return true;
        }

        String path = req.getRequestURI();
        if (WHITELIST_PATH.contains(path)) {
            return true;
        }
        for (String prefix : WHITELIST_PREFIX) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        AuthenticationToken token = createToken(request, response);
        if (token == null) {
            writeUnauthorized((HttpServletResponse) response, "请先登录");
            return false;
        }
        try {
            getSubject(request, response).login(token);
            return true;
        } catch (Exception ex) {
            log.warn("JWT authenticate failed for path={}, error={}", ((HttpServletRequest) request).getRequestURI(), ex.getMessage(), ex);
            writeUnauthorized((HttpServletResponse) response, "Token无效或已过期");
            return false;
        }
    }

    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String origin = req.getHeader("Origin");
        if (origin != null) {
            res.setHeader("Access-Control-Allow-Origin", origin);
        }
        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type");
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        return super.preHandle(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}

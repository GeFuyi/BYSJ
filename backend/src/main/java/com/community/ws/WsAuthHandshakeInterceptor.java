package com.community.ws;

import com.community.entity.SysUser;
import com.community.security.JwtUtil;
import com.community.service.SysUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
public class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "wsUserId";
    public static final String ATTR_USERNAME = "wsUsername";
    public static final String ATTR_ROLE = "wsRole";
    public static final String ATTR_NICKNAME = "wsNickname";
    public static final String ATTR_AVATAR_PATH = "wsAvatarPath";

    private final JwtUtil jwtUtil;
    private final SysUserService userService;

    public WsAuthHandshakeInterceptor(JwtUtil jwtUtil, SysUserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token) || !jwtUtil.validate(token)) {
            return false;
        }
        String username = jwtUtil.getUsername(token);
        if (!StringUtils.hasText(username)) {
            return false;
        }
        try {
            SysUser user = userService.getByUsername(username);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                return false;
            }
            attributes.put(ATTR_USER_ID, user.getId());
            attributes.put(ATTR_USERNAME, user.getUsername());
            attributes.put(ATTR_ROLE, user.getRole());
            attributes.put(ATTR_NICKNAME, user.getNickname());
            attributes.put(ATTR_AVATAR_PATH, user.getAvatarPath());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        String token = tokenFromQuery(request);
        if (StringUtils.hasText(token)) {
            return token;
        }

        HttpHeaders headers = request.getHeaders();
        String auth = headers.getFirst("Authorization");
        if (!StringUtils.hasText(auth)) {
            return null;
        }
        if (auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return auth;
    }

    private String tokenFromQuery(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest)) {
            return null;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        String token = servletRequest.getParameter("token");
        return StringUtils.hasText(token) ? token.trim() : null;
    }
}

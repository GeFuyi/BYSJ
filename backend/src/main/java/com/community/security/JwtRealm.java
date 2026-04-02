package com.community.security;

import com.community.entity.SysUser;
import com.community.service.SysUserService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

@Component
public class JwtRealm extends AuthorizingRealm {

    private final JwtUtil jwtUtil;
    private final SysUserService userService;

    public JwtRealm(JwtUtil jwtUtil, SysUserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwt = (String) token.getCredentials();
        if (!jwtUtil.validate(jwt)) {
            throw new AuthenticationException("认证失败");
        }

        String username = jwtUtil.getUsername(jwt);
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new UnknownAccountException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new AuthenticationException("认证失败");
        }
        return new SimpleAuthenticationInfo(username, jwt, getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String username = (String) principals.getPrimaryPrincipal();
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            return null;
        }
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.addRole(user.getRole());
        return info;
    }
}

package com.community.controller;

import com.community.common.ApiResponse;
import com.community.common.BusinessException;
import com.community.dto.UserRequest;
import com.community.dto.UserResponse;
import com.community.entity.SysUser;
import com.community.service.SysUserService;
import org.apache.shiro.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserService userService;

    public UserController(SysUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        String username = (String) SecurityUtils.getSubject().getPrincipal();
        SysUser user = userService.getByUsername(username);
        return ApiResponse.success(UserResponse.from(user));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        requireAnyRole("ADMIN", "EMPLOYEE");
        List<UserResponse> list = userService.listUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long id) {
        requireAnyRole("ADMIN", "EMPLOYEE");
        return ApiResponse.success(UserResponse.from(userService.getUserById(id)));
    }

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Validated @RequestBody UserRequest request) {
        requireAnyRole("ADMIN", "EMPLOYEE");
        return ApiResponse.success(UserResponse.from(userService.createUser(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long id, @Validated @RequestBody UserRequest request) {
        requireAnyRole("ADMIN", "EMPLOYEE");
        return ApiResponse.success(UserResponse.from(userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        requireRole("ADMIN");
        SysUser currentUser = currentUser();
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new BusinessException("不能删除当前登录用户");
        }
        userService.deleteUser(id);
        return ApiResponse.success();
    }

    private SysUser currentUser() {
        String username = (String) SecurityUtils.getSubject().getPrincipal();
        if (username == null) {
            throw new BusinessException(401, "未登录");
        }
        return userService.getByUsername(username);
    }

    private void requireAnyRole(String... roles) {
        SysUser user = currentUser();
        Set<String> allowed = java.util.Arrays.stream(roles).collect(java.util.stream.Collectors.toSet());
        if (!allowed.contains(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private void requireRole(String role) {
        SysUser user = currentUser();
        if (!role.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }
}

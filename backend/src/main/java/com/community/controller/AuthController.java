package com.community.controller;

import com.community.common.ApiResponse;
import com.community.dto.LoginRequest;
import com.community.dto.LoginResponse;
import com.community.dto.RegisterRequest;
import com.community.dto.SmsCodeRequest;
import com.community.dto.SmsCodeSendResponse;
import com.community.dto.SmsLoginRequest;
import com.community.dto.UserResponse;
import com.community.entity.SysUser;
import com.community.service.SysUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService userService;

    public AuthController(SysUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Validated @RequestBody RegisterRequest request) {
        SysUser user = userService.register(request);
        return ApiResponse.success(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/sms/send")
    public ApiResponse<SmsCodeSendResponse> sendSmsCode(@Validated @RequestBody SmsCodeRequest request) {
        return ApiResponse.success(userService.sendSmsCode(request));
    }

    @PostMapping("/sms/login")
    public ApiResponse<LoginResponse> smsLogin(@Validated @RequestBody SmsLoginRequest request) {
        return ApiResponse.success(userService.smsLogin(request));
    }
}

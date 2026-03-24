package com.community.service;

import com.community.dto.LoginRequest;
import com.community.dto.LoginResponse;
import com.community.dto.RegisterRequest;
import com.community.dto.SmsCodeRequest;
import com.community.dto.SmsCodeSendResponse;
import com.community.dto.SmsLoginRequest;
import com.community.dto.UserRequest;
import com.community.entity.SysUser;

import java.util.List;

public interface SysUserService {

    SysUser register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    SmsCodeSendResponse sendSmsCode(SmsCodeRequest request);

    LoginResponse smsLogin(SmsLoginRequest request);

    List<SysUser> listUsers();

    SysUser getUserById(Long id);

    SysUser createUser(UserRequest request);

    SysUser updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    SysUser getByUsername(String username);

    SysUser getByPhone(String phone);
}

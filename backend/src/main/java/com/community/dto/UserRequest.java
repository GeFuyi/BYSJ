package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRequest {

    @Size(min = 4, max = 20, message = "长度不合法")
    private String username;

    @Size(min = 6, max = 32, message = "长度不合法")
    private String password;

    @Pattern(regexp = "^\\d{11}$", message = "格式不正确")
    private String phone;

    @Size(max = 30, message = "长度不合法")
    private String nickname;

    @Size(max = 255, message = "长度不合法")
    private String avatarPath;

    @Pattern(regexp = "ADMIN|EMPLOYEE|USER", message = "格式不正确")
    private String role;

    private Integer status;
}

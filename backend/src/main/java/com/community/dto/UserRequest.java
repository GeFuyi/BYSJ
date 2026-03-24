package com.community.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UserRequest {

    @Size(min = 4, max = 20, message = "用户名长度需在4-20位")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位")
    private String password;

    @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 30, message = "昵称最多30位")
    private String nickname;

    @Pattern(regexp = "ADMIN|EMPLOYEE|USER", message = "角色仅支持ADMIN/EMPLOYEE/USER")
    private String role;

    private Integer status;
}

package com.community.controller;

import com.community.common.ApiResponse;
import com.community.common.BusinessException;
import com.community.dto.ElectricityDefaultsResponse;
import com.community.dto.ElectricityOrderCreateRequest;
import com.community.dto.ElectricityOrderResponse;
import com.community.entity.SysUser;
import com.community.service.ElectricityPaymentService;
import com.community.service.SysUserService;
import org.apache.shiro.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/electricity")
public class ElectricityPaymentController {

    private final ElectricityPaymentService electricityPaymentService;
    private final SysUserService userService;

    public ElectricityPaymentController(ElectricityPaymentService electricityPaymentService, SysUserService userService) {
        this.electricityPaymentService = electricityPaymentService;
        this.userService = userService;
    }

    @GetMapping("/defaults")
    public ApiResponse<ElectricityDefaultsResponse> defaults() {
        return ApiResponse.success(electricityPaymentService.getDefaults());
    }

    @PostMapping("/orders")
    public ApiResponse<ElectricityOrderResponse> createOrder(@Validated @RequestBody ElectricityOrderCreateRequest request) {
        return ApiResponse.success(electricityPaymentService.createOrder(request, currentUser()));
    }

    @GetMapping("/orders/my")
    public ApiResponse<List<ElectricityOrderResponse>> myOrders() {
        return ApiResponse.success(electricityPaymentService.myOrders(currentUser()));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<ElectricityOrderResponse> order(@PathVariable Long id) {
        return ApiResponse.success(electricityPaymentService.getOrder(id, currentUser()));
    }

    @PostMapping("/orders/{id}/refresh")
    public ApiResponse<ElectricityOrderResponse> refresh(@PathVariable Long id) {
        return ApiResponse.success(electricityPaymentService.refreshOrderStatus(id, currentUser()));
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        return electricityPaymentService.handleNotify(request);
    }

    private SysUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userService.getByUsername(String.valueOf(principal));
    }
}


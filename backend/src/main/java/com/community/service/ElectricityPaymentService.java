package com.community.service;

import com.community.dto.ElectricityDefaultsResponse;
import com.community.dto.ElectricityOrderCreateRequest;
import com.community.dto.ElectricityOrderResponse;
import com.community.entity.SysUser;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ElectricityPaymentService {

    ElectricityDefaultsResponse getDefaults();

    ElectricityOrderResponse createOrder(ElectricityOrderCreateRequest request, SysUser currentUser);

    List<ElectricityOrderResponse> myOrders(SysUser currentUser);

    ElectricityOrderResponse getOrder(Long id, SysUser currentUser);

    ElectricityOrderResponse refreshOrderStatus(Long id, SysUser currentUser);

    String handleNotify(HttpServletRequest request);
}


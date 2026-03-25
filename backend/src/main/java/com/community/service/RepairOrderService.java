package com.community.service;

import com.community.dto.RepairImageUploadResponse;
import com.community.dto.RepairOrderCreateRequest;
import com.community.dto.RepairOrderDetailResponse;
import com.community.dto.RepairOrderListItemResponse;
import com.community.dto.RepairOrderStatusUpdateRequest;
import com.community.entity.SysUser;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RepairOrderService {

    RepairImageUploadResponse uploadImage(MultipartFile file);

    RepairOrderDetailResponse createOrder(RepairOrderCreateRequest request, SysUser currentUser);

    List<RepairOrderListItemResponse> listOrders(String status, SysUser currentUser);

    RepairOrderDetailResponse getOrderDetail(Long id, SysUser currentUser);

    RepairOrderDetailResponse updateStatus(Long id, RepairOrderStatusUpdateRequest request, SysUser currentUser);

    Resource loadImageAsResource(String path);
}


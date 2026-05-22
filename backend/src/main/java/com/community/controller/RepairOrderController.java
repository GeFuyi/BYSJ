package com.community.controller;

import com.community.common.ApiResponse;
import com.community.common.BusinessException;
import com.community.dto.RepairImageUploadResponse;
import com.community.dto.RepairOrderCreateRequest;
import com.community.dto.RepairOrderDetailResponse;
import com.community.dto.RepairOrderListItemResponse;
import com.community.dto.RepairOrderStatusUpdateRequest;
import com.community.entity.SysUser;
import com.community.service.RepairOrderService;
import com.community.service.SysUserService;
import org.apache.shiro.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/repair")
public class RepairOrderController {

    private final RepairOrderService repairOrderService;
    private final SysUserService userService;

    public RepairOrderController(RepairOrderService repairOrderService, SysUserService userService) {
        this.repairOrderService = repairOrderService;
        this.userService = userService;
    }

    @PostMapping("/upload-image")
    public ApiResponse<RepairImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(repairOrderService.uploadImage(file));
    }

    @PostMapping(value = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<RepairOrderDetailResponse> createOrder(@Validated @RequestBody RepairOrderCreateRequest request) {
        return ApiResponse.success(repairOrderService.createOrder(request, currentUser()));
    }

    @GetMapping("/orders")
    public ApiResponse<List<RepairOrderListItemResponse>> listOrders(@RequestParam(value = "status", required = false) String status,
                                                                      @RequestParam(value = "mineOnly", required = false) Boolean mineOnly) {
        return ApiResponse.success(repairOrderService.listOrders(status, mineOnly, currentUser()));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<RepairOrderDetailResponse> orderDetail(@PathVariable Long id) {
        return ApiResponse.success(repairOrderService.getOrderDetail(id, currentUser()));
    }

    @PutMapping(value = "/orders/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<RepairOrderDetailResponse> updateStatus(@PathVariable Long id,
                                                               @Validated @RequestBody RepairOrderStatusUpdateRequest request) {
        return ApiResponse.success(repairOrderService.updateStatus(id, request, currentUser()));
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getImage(@RequestParam("path") String path) {
        Resource resource = repairOrderService.loadImageAsResource(path);
        String contentType = "application/octet-stream";
        try {
            contentType = resource.getFile() == null
                    ? contentType
                    : java.nio.file.Files.probeContentType(resource.getFile().toPath());
        } catch (IOException ignored) {
            // ignore and use default content type
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                .body(resource);
    }

    private SysUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userService.getByUsername(String.valueOf(principal));
    }
}

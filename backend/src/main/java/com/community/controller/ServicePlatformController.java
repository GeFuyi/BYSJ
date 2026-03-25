package com.community.controller;

import com.community.common.ApiResponse;
import com.community.common.BusinessException;
import com.community.dto.ServiceAuditRequest;
import com.community.dto.ServiceBookingCreateRequest;
import com.community.dto.ServiceBookingResponse;
import com.community.dto.ServiceCategoryResponse;
import com.community.dto.ServiceDetailResponse;
import com.community.dto.ServiceEntryRequest;
import com.community.dto.ServiceImageUploadResponse;
import com.community.dto.ServiceListItemResponse;
import com.community.dto.ServiceOperateStatusUpdateRequest;
import com.community.dto.ServiceReviewCreateRequest;
import com.community.dto.ServiceReviewResponse;
import com.community.entity.SysUser;
import com.community.service.ServicePlatformService;
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
@RequestMapping("/api/services")
public class ServicePlatformController {

    private final ServicePlatformService servicePlatformService;
    private final SysUserService userService;

    public ServicePlatformController(ServicePlatformService servicePlatformService, SysUserService userService) {
        this.servicePlatformService = servicePlatformService;
        this.userService = userService;
    }

    @PostMapping("/upload-image")
    public ApiResponse<ServiceImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(servicePlatformService.uploadImage(file));
    }

    @GetMapping("/file")
    public ResponseEntity<Resource> getImage(@RequestParam("path") String path) {
        Resource resource = servicePlatformService.loadImageAsResource(path);
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

    @GetMapping("/categories")
    public ApiResponse<List<ServiceCategoryResponse>> categories() {
        return ApiResponse.success(servicePlatformService.listCategories());
    }

    @PostMapping("/provider/entries")
    public ApiResponse<ServiceDetailResponse> createEntry(@Validated @RequestBody ServiceEntryRequest request) {
        return ApiResponse.success(servicePlatformService.createEntry(request, currentUser()));
    }

    @PutMapping("/provider/entries/{id}")
    public ApiResponse<ServiceDetailResponse> updateEntry(@PathVariable Long id, @Validated @RequestBody ServiceEntryRequest request) {
        return ApiResponse.success(servicePlatformService.updateEntry(id, request, currentUser()));
    }

    @GetMapping("/provider/entries")
    public ApiResponse<List<ServiceListItemResponse>> providerEntries(@RequestParam(value = "auditStatus", required = false) String auditStatus) {
        return ApiResponse.success(servicePlatformService.listProviderEntries(auditStatus, currentUser()));
    }

    @PutMapping("/provider/entries/{id}/operate-status")
    public ApiResponse<ServiceDetailResponse> updateOperateStatus(@PathVariable Long id,
                                                                  @Validated @RequestBody ServiceOperateStatusUpdateRequest request) {
        return ApiResponse.success(servicePlatformService.updateOperateStatus(id, request, currentUser()));
    }

    @GetMapping("/audit/entries")
    public ApiResponse<List<ServiceListItemResponse>> auditEntries(@RequestParam(value = "auditStatus", required = false) String auditStatus,
                                                                   @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResponse.success(servicePlatformService.listAuditEntries(auditStatus, keyword, currentUser()));
    }

    @PutMapping("/audit/entries/{id}")
    public ApiResponse<ServiceDetailResponse> audit(@PathVariable Long id, @Validated @RequestBody ServiceAuditRequest request) {
        return ApiResponse.success(servicePlatformService.auditEntry(id, request, currentUser()));
    }

    @GetMapping("/list")
    public ApiResponse<List<ServiceListItemResponse>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                                           @RequestParam(value = "categoryCode", required = false) String categoryCode,
                                                           @RequestParam(value = "serviceStatus", required = false) String serviceStatus) {
        return ApiResponse.success(servicePlatformService.listPublishedServices(keyword, categoryCode, serviceStatus));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(servicePlatformService.serviceDetail(id, currentUserNullable()));
    }

    @PostMapping("/{id}/bookings")
    public ApiResponse<ServiceBookingResponse> booking(@PathVariable Long id, @Validated @RequestBody ServiceBookingCreateRequest request) {
        return ApiResponse.success(servicePlatformService.createBooking(id, request, currentUser()));
    }

    @GetMapping("/my/bookings")
    public ApiResponse<List<ServiceBookingResponse>> myBookings() {
        return ApiResponse.success(servicePlatformService.myBookings(currentUser()));
    }

    @PostMapping("/{id}/reviews")
    public ApiResponse<ServiceReviewResponse> review(@PathVariable Long id, @Validated @RequestBody ServiceReviewCreateRequest request) {
        return ApiResponse.success(servicePlatformService.submitReview(id, request, currentUser()));
    }

    @GetMapping("/{id}/reviews")
    public ApiResponse<List<ServiceReviewResponse>> reviews(@PathVariable Long id) {
        return ApiResponse.success(servicePlatformService.listReviews(id));
    }

    private SysUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userService.getByUsername(String.valueOf(principal));
    }

    private SysUser currentUserNullable() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal == null) {
            return null;
        }
        return userService.getByUsername(String.valueOf(principal));
    }
}


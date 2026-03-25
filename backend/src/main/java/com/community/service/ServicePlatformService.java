package com.community.service;

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
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServicePlatformService {

    ServiceImageUploadResponse uploadImage(MultipartFile file);

    List<ServiceCategoryResponse> listCategories();

    ServiceDetailResponse createEntry(ServiceEntryRequest request, SysUser currentUser);

    ServiceDetailResponse updateEntry(Long id, ServiceEntryRequest request, SysUser currentUser);

    List<ServiceListItemResponse> listProviderEntries(String auditStatus, SysUser currentUser);

    List<ServiceListItemResponse> listAuditEntries(String auditStatus, String keyword, SysUser currentUser);

    ServiceDetailResponse auditEntry(Long id, ServiceAuditRequest request, SysUser currentUser);

    ServiceDetailResponse updateOperateStatus(Long id, ServiceOperateStatusUpdateRequest request, SysUser currentUser);

    List<ServiceListItemResponse> listPublishedServices(String keyword, String categoryCode, String serviceStatus);

    ServiceDetailResponse serviceDetail(Long id, SysUser currentUser);

    ServiceBookingResponse createBooking(Long serviceId, ServiceBookingCreateRequest request, SysUser currentUser);

    List<ServiceBookingResponse> myBookings(SysUser currentUser);

    ServiceReviewResponse submitReview(Long serviceId, ServiceReviewCreateRequest request, SysUser currentUser);

    List<ServiceReviewResponse> listReviews(Long serviceId);

    Resource loadImageAsResource(String path);
}


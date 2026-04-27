package com.community.service.impl;

import com.community.common.BusinessException;
import com.community.config.ServicePlatformProperties;
import com.community.dto.ServiceAuditLogResponse;
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
import com.community.entity.ConvenienceService;
import com.community.entity.ServiceAuditLog;
import com.community.entity.ServiceBooking;
import com.community.entity.ServiceCategory;
import com.community.entity.ServiceImage;
import com.community.entity.ServiceReview;
import com.community.entity.SysUser;
import com.community.enums.ServiceAuditAction;
import com.community.enums.ServiceAuditStatus;
import com.community.enums.ServiceBookingStatus;
import com.community.enums.ServiceOperateStatus;
import com.community.mapper.ConvenienceServiceMapper;
import com.community.mapper.ServiceAuditLogMapper;
import com.community.mapper.ServiceBookingMapper;
import com.community.mapper.ServiceCategoryMapper;
import com.community.mapper.ServiceImageMapper;
import com.community.mapper.ServiceReviewMapper;
import com.community.service.ServicePlatformService;
import com.community.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServicePlatformServiceImpl implements ServicePlatformService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_USER = "USER";
    private static final String CATEGORY_LIST_KEY = "svc:category:list:enabled";
    private static final String CATEGORY_KEY_PREFIX = "svc:category:code:";
    private static final String PUBLISHED_VERSION_KEY = "svc:published:version";
    private static final String PUBLISHED_LIST_KEY_PREFIX = "svc:published:list:";
    private static final String DETAIL_PUBLIC_KEY_PREFIX = "svc:detail:public:";
    private static final String DETAIL_AUDIT_KEY_PREFIX = "svc:detail:audit:";
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(30);
    private static final Duration PUBLISHED_LIST_TTL = Duration.ofMinutes(5);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(5);

    private final ServicePlatformProperties properties;
    private final ServiceCategoryMapper categoryMapper;
    private final ConvenienceServiceMapper serviceMapper;
    private final ServiceImageMapper imageMapper;
    private final ServiceAuditLogMapper auditLogMapper;
    private final ServiceBookingMapper bookingMapper;
    private final ServiceReviewMapper reviewMapper;
    private final SysUserService sysUserService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ServicePlatformServiceImpl(ServicePlatformProperties properties,
                                      ServiceCategoryMapper categoryMapper,
                                      ConvenienceServiceMapper serviceMapper,
                                      ServiceImageMapper imageMapper,
                                      ServiceAuditLogMapper auditLogMapper,
                                      ServiceBookingMapper bookingMapper,
                                      ServiceReviewMapper reviewMapper,
                                      SysUserService sysUserService,
                                      RedisTemplate<String, Object> redisTemplate,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.categoryMapper = categoryMapper;
        this.serviceMapper = serviceMapper;
        this.imageMapper = imageMapper;
        this.auditLogMapper = auditLogMapper;
        this.bookingMapper = bookingMapper;
        this.reviewMapper = reviewMapper;
        this.sysUserService = sysUserService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ServiceImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("操作失败");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("操作失败");
        }
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("操作失败");
        }

        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = getUploadRoot().resolve(day).normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
        String suffix = fileSuffix(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        Path target = dir.resolve(fileName).normalize();
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
        String relativePath = day + "/" + fileName;
        ServiceImageUploadResponse response = new ServiceImageUploadResponse();
        response.setPath(relativePath);
        response.setOriginalName(file.getOriginalFilename());
        response.setUrl("/api/services/file?path=" + URLEncoder.encode(relativePath, StandardCharsets.UTF_8));
        return response;
    }

    @Override
    public List<ServiceCategoryResponse> listCategories() {
        Object cached = redisTemplate.opsForValue().get(CATEGORY_LIST_KEY);
        if (cached != null) {
            return convertToCategoryResponseList(cached);
        }
        List<ServiceCategoryResponse> categories = categoryMapper.selectEnabled().stream().map(this::toCategoryResponse).collect(Collectors.toList());
        redisTemplate.opsForValue().set(CATEGORY_LIST_KEY, categories, CATEGORY_TTL);
        return categories;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceDetailResponse createEntry(ServiceEntryRequest request, SysUser currentUser) {
        requireProviderRole(currentUser);
        validateCategory(request.getCategoryCode());

        ConvenienceService service = new ConvenienceService();
        service.setProviderId(currentUser.getId());
        service.setName(request.getName());
        service.setCategoryCode(request.getCategoryCode());
        service.setSummary(request.getSummary());
        service.setDescription(request.getDescription());
        service.setContactName(request.getContactName());
        service.setContactPhone(request.getContactPhone());
        service.setAddress(request.getAddress());
        service.setCoverImagePath(request.getCoverImagePath());
        service.setServiceStatus(ServiceOperateStatus.RESERVABLE.name());
        service.setAuditStatus(ServiceAuditStatus.PENDING.name());
        service.setAuditReason(null);
        service.setMaxCapacity(request.getMaxCapacity() == null ? 50 : request.getMaxCapacity());
        service.setCurrentBooked(0);
        service.setAvgScore(BigDecimal.ZERO);
        service.setScoreCount(0);
        serviceMapper.insert(service);

        List<String> imagePaths = sanitizeImagePaths(request.getImagePaths());
        if (!imagePaths.isEmpty()) {
            imageMapper.insertBatch(service.getId(), imagePaths);
            if (!StringUtils.hasText(service.getCoverImagePath())) {
                service.setCoverImagePath(imagePaths.get(0));
                serviceMapper.updateForResubmit(service);
            }
        }
        invalidateServiceCaches(service.getId());
        return buildDetail(serviceMapper.selectById(service.getId()), currentUser, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceDetailResponse updateEntry(Long id, ServiceEntryRequest request, SysUser currentUser) {
        requireProviderRole(currentUser);
        validateCategory(request.getCategoryCode());
        ConvenienceService existing = serviceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ROLE_ADMIN.equals(currentUser.getRole()) && !existing.getProviderId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权限访问");
        }
        int maxCapacity = request.getMaxCapacity() == null ? existing.getMaxCapacity() : request.getMaxCapacity();
        if (existing.getCurrentBooked() != null && maxCapacity < existing.getCurrentBooked()) {
            throw new BusinessException("操作失败");
        }

        existing.setName(request.getName());
        existing.setCategoryCode(request.getCategoryCode());
        existing.setSummary(request.getSummary());
        existing.setDescription(request.getDescription());
        existing.setContactName(request.getContactName());
        existing.setContactPhone(request.getContactPhone());
        existing.setAddress(request.getAddress());
        existing.setCoverImagePath(request.getCoverImagePath());
        existing.setMaxCapacity(maxCapacity);
        existing.setAuditStatus(ServiceAuditStatus.PENDING.name());
        existing.setAuditReason(null);
        serviceMapper.updateForResubmit(existing);

        imageMapper.deleteByServiceId(id);
        List<String> imagePaths = sanitizeImagePaths(request.getImagePaths());
        if (!imagePaths.isEmpty()) {
            imageMapper.insertBatch(id, imagePaths);
            if (!StringUtils.hasText(existing.getCoverImagePath())) {
                existing.setCoverImagePath(imagePaths.get(0));
                serviceMapper.updateForResubmit(existing);
            }
        }
        invalidateServiceCaches(id);
        return buildDetail(serviceMapper.selectById(id), currentUser, true);
    }

    @Override
    public List<ServiceListItemResponse> listProviderEntries(String auditStatus, SysUser currentUser) {
        requireProviderRole(currentUser);
        String normalizedAuditStatus = normalizeAuditStatus(auditStatus);
        Long providerId = ROLE_ADMIN.equals(currentUser.getRole()) ? null : currentUser.getId();
        List<ConvenienceService> services = serviceMapper.selectProviderList(providerId, normalizedAuditStatus);
        return services.stream().map(this::toListItem).collect(Collectors.toList());
    }

    @Override
    public List<ServiceListItemResponse> listAuditEntries(String auditStatus, String keyword, SysUser currentUser) {
        requireAdminRole(currentUser);
        String normalizedAuditStatus = normalizeAuditStatus(auditStatus);
        String safeKeyword = keyword == null ? null : keyword.trim();
        List<ConvenienceService> services = serviceMapper.selectAuditList(normalizedAuditStatus, safeKeyword);
        return services.stream().map(this::toListItem).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceDetailResponse auditEntry(Long id, ServiceAuditRequest request, SysUser currentUser) {
        requireAdminRole(currentUser);
        ConvenienceService service = serviceMapper.selectById(id);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ServiceAuditStatus.PENDING.name().equals(service.getAuditStatus())) {
            throw new BusinessException("操作失败");
        }

        ServiceAuditAction action = ServiceAuditAction.fromCode(request.getAction().trim().toUpperCase(Locale.ROOT));
        String toAuditStatus;
        String reason = request.getReason();
        if (action == ServiceAuditAction.APPROVE) {
            toAuditStatus = ServiceAuditStatus.APPROVED.name();
            reason = null;
        } else if (action == ServiceAuditAction.REJECT) {
            toAuditStatus = ServiceAuditStatus.REJECTED.name();
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException("操作失败");
            }
        } else {
            toAuditStatus = ServiceAuditStatus.RETURNED.name();
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException("操作失败");
            }
        }

        serviceMapper.updateAuditResult(id, toAuditStatus, reason, currentUser.getId());

        ServiceAuditLog log = new ServiceAuditLog();
        log.setServiceId(id);
        log.setFromAuditStatus(service.getAuditStatus());
        log.setToAuditStatus(toAuditStatus);
        log.setAction(action.name());
        log.setReason(reason);
        log.setReviewerId(currentUser.getId());
        log.setReviewerName(resolveDisplayName(currentUser));
        auditLogMapper.insert(log);

        invalidateServiceCaches(id);
        return buildDetail(serviceMapper.selectById(id), currentUser, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceDetailResponse updateOperateStatus(Long id, ServiceOperateStatusUpdateRequest request, SysUser currentUser) {
        requireProviderRole(currentUser);
        ConvenienceService service = serviceMapper.selectById(id);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ROLE_ADMIN.equals(currentUser.getRole()) && !service.getProviderId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权限访问");
        }
        if (!ServiceAuditStatus.APPROVED.name().equals(service.getAuditStatus())) {
            throw new BusinessException("操作失败");
        }

        String targetStatus = normalizeServiceStatus(request.getServiceStatus());
        if (ServiceOperateStatus.RESERVABLE.name().equals(targetStatus)
                && service.getCurrentBooked() != null
                && service.getMaxCapacity() != null
                && service.getCurrentBooked() >= service.getMaxCapacity()) {
            throw new BusinessException("操作失败");
        }
        serviceMapper.updateOperateStatus(id, targetStatus);
        invalidateServiceCaches(id);
        return buildDetail(serviceMapper.selectById(id), currentUser, true);
    }

    @Override
    public List<ServiceListItemResponse> listPublishedServices(String keyword, String categoryCode, String serviceStatus) {
        String safeKeyword = normalizeKeyword(keyword);
        String safeCategoryCode = normalizeCategoryCode(categoryCode);
        if (StringUtils.hasText(safeCategoryCode)) {
            validateCategory(safeCategoryCode);
        }
        String normalizedServiceStatus = normalizeServiceStatus(serviceStatus);
        String cacheKey = publishedListKey(currentPublishedVersion(), normalizedServiceStatus, safeCategoryCode, safeKeyword);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return convertToServiceListItemResponseList(cached);
        }
        List<ServiceListItemResponse> responses = serviceMapper.selectPublishedList(safeKeyword, safeCategoryCode, normalizedServiceStatus)
                .stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
        redisTemplate.opsForValue().set(cacheKey, responses, PUBLISHED_LIST_TTL);
        return responses;
    }

    @Override
    public ServiceDetailResponse serviceDetail(Long id, SysUser currentUser) {
        ConvenienceService service = serviceMapper.selectById(id);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        boolean canViewAuditDetails = canViewAudit(service, currentUser);
        if (!ServiceAuditStatus.APPROVED.name().equals(service.getAuditStatus()) && !canViewAuditDetails) {
            throw new BusinessException(403, "无权限访问");
        }
        String cacheKey = canViewAuditDetails ? detailAuditKey(id) : detailPublicKey(id);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return objectMapper.convertValue(cached, ServiceDetailResponse.class);
        }
        ServiceDetailResponse detail = buildDetail(service, currentUser, canViewAuditDetails);
        redisTemplate.opsForValue().set(cacheKey, detail, DETAIL_TTL);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceBookingResponse createBooking(Long serviceId, ServiceBookingCreateRequest request, SysUser currentUser) {
        requireUserRole(currentUser);
        ConvenienceService service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ServiceAuditStatus.APPROVED.name().equals(service.getAuditStatus())) {
            throw new BusinessException("操作失败");
        }
        if (!ServiceOperateStatus.RESERVABLE.name().equals(service.getServiceStatus())) {
            throw new BusinessException("操作失败");
        }
        ServiceBooking existing = bookingMapper.selectActiveByServiceAndUser(serviceId, currentUser.getId());
        if (existing != null) {
            throw new BusinessException("操作失败");
        }

        ServiceBooking booking = new ServiceBooking();
        booking.setServiceId(serviceId);
        booking.setUserId(currentUser.getId());
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setRemark(request.getRemark());
        booking.setStatus(ServiceBookingStatus.BOOKED.name());
        bookingMapper.insert(booking);

        int affected = serviceMapper.incrementBookedIfAvailable(serviceId);
        if (affected <= 0) {
            throw new BusinessException("操作失败");
        }

        invalidateServiceCaches(serviceId);
        return toBookingResponse(booking, service);
    }

    @Override
    public List<ServiceBookingResponse> myBookings(SysUser currentUser) {
        requireUserRole(currentUser);
        List<ServiceBooking> bookings = bookingMapper.selectByUserId(currentUser.getId());
        List<ServiceBookingResponse> responses = new ArrayList<>();
        for (ServiceBooking booking : bookings) {
            ConvenienceService service = serviceMapper.selectById(booking.getServiceId());
            responses.add(toBookingResponse(booking, service));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceReviewResponse submitReview(Long serviceId, ServiceReviewCreateRequest request, SysUser currentUser) {
        requireUserRole(currentUser);
        ConvenienceService service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ServiceAuditStatus.APPROVED.name().equals(service.getAuditStatus())) {
            throw new BusinessException("操作失败");
        }
        ServiceBooking booking = bookingMapper.selectActiveByServiceAndUser(serviceId, currentUser.getId());
        if (booking == null) {
            throw new BusinessException("操作失败");
        }
        ServiceReview existed = reviewMapper.selectByServiceAndUser(serviceId, currentUser.getId());
        if (existed != null) {
            throw new BusinessException("操作失败");
        }

        ServiceReview review = new ServiceReview();
        review.setServiceId(serviceId);
        review.setUserId(currentUser.getId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setReviewerName(resolveDisplayName(currentUser));
        reviewMapper.insert(review);
        serviceMapper.refreshScore(serviceId);
        invalidateServiceCaches(serviceId);
        return toReviewResponse(review);
    }

    @Override
    public List<ServiceReviewResponse> listReviews(Long serviceId) {
        ConvenienceService service = serviceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(404, "资源不存在");
        }
        return reviewMapper.selectByServiceId(serviceId).stream().map(this::toReviewResponse).collect(Collectors.toList());
    }

    @Override
    public Resource loadImageAsResource(String path) {
        if (!StringUtils.hasText(path) || path.contains("..")) {
            throw new BusinessException(400, "请求参数不合法");
        }
        Path target = getUploadRoot().resolve(path).normalize();
        if (!target.startsWith(getUploadRoot())) {
            throw new BusinessException(400, "请求参数不合法");
        }
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new BusinessException(404, "资源不存在");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
    }

    private ServiceDetailResponse buildDetail(ConvenienceService service, SysUser currentUser, boolean includeAuditLogs) {
        ServiceDetailResponse response = new ServiceDetailResponse();
        copyBase(response, service);
        response.setDescription(service.getDescription());
        response.setAddress(service.getAddress());
        response.setImagePaths(imageMapper.selectByServiceId(service.getId()).stream().map(ServiceImage::getImagePath).collect(Collectors.toList()));
        response.setReviews(reviewMapper.selectByServiceId(service.getId()).stream().map(this::toReviewResponse).collect(Collectors.toList()));
        if (includeAuditLogs) {
            response.setAuditLogs(auditLogMapper.selectByServiceId(service.getId()).stream()
                    .map(this::toAuditLogResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setAuditLogs(new ArrayList<>());
        }
        return response;
    }

    private ServiceListItemResponse toListItem(ConvenienceService service) {
        ServiceListItemResponse response = new ServiceListItemResponse();
        copyBase(response, service);
        return response;
    }

    private void copyBase(ServiceListItemResponse response, ConvenienceService service) {
        SysUser provider = service.getProviderId() == null ? null : loadUser(service.getProviderId());
        ServiceCategory category = loadCategory(service.getCategoryCode());

        response.setId(service.getId());
        response.setProviderId(service.getProviderId());
        response.setProviderName(provider == null ? "-" : resolveDisplayName(provider));
        response.setProviderAvatarPath(provider == null ? null : provider.getAvatarPath());
        response.setName(service.getName());
        response.setCategoryCode(service.getCategoryCode());
        response.setCategoryName(category == null ? service.getCategoryCode() : category.getName());
        response.setSummary(service.getSummary());
        response.setContactName(service.getContactName());
        response.setContactPhone(service.getContactPhone());
        response.setCoverImagePath(service.getCoverImagePath());
        response.setServiceStatus(service.getServiceStatus());
        response.setServiceStatusLabel(serviceStatusLabel(service.getServiceStatus()));
        response.setAuditStatus(service.getAuditStatus());
        response.setAuditStatusLabel(auditStatusLabel(service.getAuditStatus()));
        response.setAuditReason(service.getAuditReason());
        response.setMaxCapacity(service.getMaxCapacity());
        response.setCurrentBooked(service.getCurrentBooked());
        response.setAvgScore(service.getAvgScore() == null ? BigDecimal.ZERO : service.getAvgScore());
        response.setScoreCount(service.getScoreCount() == null ? 0 : service.getScoreCount());
        response.setCreatedAt(service.getCreatedAt());
        response.setUpdatedAt(service.getUpdatedAt());
    }

    private ServiceBookingResponse toBookingResponse(ServiceBooking booking, ConvenienceService service) {
        SysUser bookingUser = booking.getUserId() == null ? null : loadUser(booking.getUserId());
        ServiceBookingResponse response = new ServiceBookingResponse();
        response.setId(booking.getId());
        response.setServiceId(booking.getServiceId());
        response.setUserId(booking.getUserId());
        response.setUserNickname(bookingUser == null ? "-" : resolveDisplayName(bookingUser));
        response.setUserAvatarPath(bookingUser == null ? null : bookingUser.getAvatarPath());
        response.setServiceName(service == null ? "-" : service.getName());
        response.setContactName(booking.getContactName());
        response.setContactPhone(booking.getContactPhone());
        response.setRemark(booking.getRemark());
        response.setStatus(booking.getStatus());
        response.setStatusLabel(bookingStatusLabel(booking.getStatus()));
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    private ServiceReviewResponse toReviewResponse(ServiceReview review) {
        SysUser reviewUser = review.getUserId() == null ? null : loadUser(review.getUserId());
        ServiceReviewResponse response = new ServiceReviewResponse();
        response.setId(review.getId());
        response.setServiceId(review.getServiceId());
        response.setUserId(review.getUserId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setReviewerName(review.getReviewerName());
        response.setReviewerAvatarPath(reviewUser == null ? null : reviewUser.getAvatarPath());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    private ServiceAuditLogResponse toAuditLogResponse(ServiceAuditLog log) {
        SysUser reviewer = log.getReviewerId() == null ? null : loadUser(log.getReviewerId());
        ServiceAuditLogResponse response = new ServiceAuditLogResponse();
        response.setId(log.getId());
        response.setFromAuditStatus(log.getFromAuditStatus());
        response.setFromAuditStatusLabel(auditStatusLabel(log.getFromAuditStatus()));
        response.setToAuditStatus(log.getToAuditStatus());
        response.setToAuditStatusLabel(auditStatusLabel(log.getToAuditStatus()));
        response.setAction(log.getAction());
        response.setReason(log.getReason());
        response.setReviewerId(log.getReviewerId());
        response.setReviewerName(log.getReviewerName());
        response.setReviewerAvatarPath(reviewer == null ? null : reviewer.getAvatarPath());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory category) {
        ServiceCategoryResponse response = new ServiceCategoryResponse();
        response.setCode(category.getCode());
        response.setName(category.getName());
        response.setSort(category.getSort());
        return response;
    }

    private SysUser loadUser(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return sysUserService.getUserById(userId);
        } catch (BusinessException ex) {
            return null;
        }
    }

    private ServiceCategory loadCategory(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            return null;
        }
        String key = categoryKey(categoryCode.trim());
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return objectMapper.convertValue(cached, ServiceCategory.class);
        }
        ServiceCategory category = categoryMapper.selectByCode(categoryCode.trim());
        if (category != null) {
            redisTemplate.opsForValue().set(key, category, CATEGORY_TTL);
        }
        return category;
    }

    private List<ServiceCategoryResponse> convertToCategoryResponseList(Object cached) {
        return convertList(cached, ServiceCategoryResponse.class);
    }

    private List<ServiceListItemResponse> convertToServiceListItemResponseList(Object cached) {
        return convertList(cached, ServiceListItemResponse.class);
    }

    private <T> List<T> convertList(Object cached, Class<T> itemClass) {
        List<?> rawList = objectMapper.convertValue(cached, List.class);
        List<T> result = new ArrayList<>();
        for (Object item : rawList) {
            result.add(objectMapper.convertValue(item, itemClass));
        }
        return result;
    }

    private void invalidateServiceCaches(Long serviceId) {
        incrementPublishedVersion();
        if (serviceId == null) {
            return;
        }
        redisTemplate.delete(detailPublicKey(serviceId));
        redisTemplate.delete(detailAuditKey(serviceId));
    }

    private long currentPublishedVersion() {
        Object cached = redisTemplate.opsForValue().get(PUBLISHED_VERSION_KEY);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        if (cached != null) {
            Long value = objectMapper.convertValue(cached, Long.class);
            if (value != null && value > 0) {
                return value;
            }
        }
        redisTemplate.opsForValue().set(PUBLISHED_VERSION_KEY, 1L);
        return 1L;
    }

    private void incrementPublishedVersion() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(PUBLISHED_VERSION_KEY))) {
            redisTemplate.opsForValue().set(PUBLISHED_VERSION_KEY, 1L);
        }
        redisTemplate.opsForValue().increment(PUBLISHED_VERSION_KEY);
    }

    private String publishedListKey(long version, String serviceStatus, String categoryCode, String keyword) {
        return PUBLISHED_LIST_KEY_PREFIX + "v" + version + ":"
                + (StringUtils.hasText(serviceStatus) ? serviceStatus : "all") + ":"
                + (StringUtils.hasText(categoryCode) ? categoryCode : "all") + ":"
                + keywordHash(keyword);
    }

    private String detailPublicKey(Long serviceId) {
        return DETAIL_PUBLIC_KEY_PREFIX + serviceId;
    }

    private String detailAuditKey(Long serviceId) {
        return DETAIL_AUDIT_KEY_PREFIX + serviceId;
    }

    private String categoryKey(String categoryCode) {
        return CATEGORY_KEY_PREFIX + categoryCode;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeCategoryCode(String categoryCode) {
        return StringUtils.hasText(categoryCode) ? categoryCode.trim() : null;
    }

    private String keywordHash(String keyword) {
        return StringUtils.hasText(keyword) ? Integer.toHexString(keyword.hashCode()) : "all";
    }

    private String serviceStatusLabel(String code) {
        try {
            return ServiceOperateStatus.fromCode(code).getLabel();
        } catch (Exception ex) {
            return code;
        }
    }

    private String auditStatusLabel(String code) {
        try {
            return ServiceAuditStatus.fromCode(code).getLabel();
        } catch (Exception ex) {
            return code;
        }
    }

    private String bookingStatusLabel(String code) {
        try {
            return ServiceBookingStatus.fromCode(code).getLabel();
        } catch (Exception ex) {
            return code;
        }
    }

    private boolean canViewAudit(ConvenienceService service, SysUser user) {
        if (user == null) {
            return false;
        }
        if (ROLE_ADMIN.equals(user.getRole())) {
            return true;
        }
        return Objects.equals(service.getProviderId(), user.getId());
    }

    private void requireProviderRole(SysUser user) {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!ROLE_ADMIN.equals(user.getRole()) && !ROLE_EMPLOYEE.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private void requireAdminRole(SysUser user) {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private void requireUserRole(SysUser user) {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!ROLE_USER.equals(user.getRole()) && !ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private void validateCategory(String categoryCode) {
        if (!StringUtils.hasText(categoryCode)) {
            throw new BusinessException("操作失败");
        }
        ServiceCategory category = loadCategory(categoryCode);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException("操作失败");
        }
    }

    private String normalizeAuditStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return ServiceAuditStatus.fromCode(status.trim().toUpperCase(Locale.ROOT)).name();
    }

    private String normalizeServiceStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return ServiceOperateStatus.fromCode(status.trim().toUpperCase(Locale.ROOT)).name();
    }

    private List<String> sanitizeImagePaths(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        return imagePaths.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(path -> !path.contains(".."))
                .distinct()
                .collect(Collectors.toList());
    }

    private String resolveDisplayName(SysUser user) {
        if (user == null) {
            return "-";
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String fileSuffix(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".jpg";
        }
        int index = fileName.lastIndexOf(".");
        if (index < 0 || index == fileName.length() - 1) {
            return ".jpg";
        }
        String suffix = fileName.substring(index).toLowerCase(Locale.ROOT);
        if (suffix.length() > 10) {
            return ".jpg";
        }
        return suffix;
    }

    private Path getUploadRoot() {
        Path root = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new BusinessException(500, "服务器内部错误");
        }
        return root;
    }
}

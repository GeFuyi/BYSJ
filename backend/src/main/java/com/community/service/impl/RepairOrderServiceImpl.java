package com.community.service.impl;

import com.community.common.BusinessException;
import com.community.config.RepairProperties;
import com.community.dto.RepairImageUploadResponse;
import com.community.dto.RepairOrderCreateRequest;
import com.community.dto.RepairOrderDetailResponse;
import com.community.dto.RepairOrderFlowResponse;
import com.community.dto.RepairOrderListItemResponse;
import com.community.dto.RepairOrderStatusUpdateRequest;
import com.community.entity.RepairOrder;
import com.community.entity.RepairOrderFlow;
import com.community.entity.RepairOrderImage;
import com.community.entity.SysUser;
import com.community.enums.RepairOrderStatus;
import com.community.mapper.RepairOrderFlowMapper;
import com.community.mapper.RepairOrderImageMapper;
import com.community.mapper.RepairOrderMapper;
import com.community.mapper.SysUserMapper;
import com.community.service.RepairOrderService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RepairOrderServiceImpl implements RepairOrderService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EMPLOYEE = "EMPLOYEE";
    private static final String ROLE_USER = "USER";

    private final RepairOrderMapper repairOrderMapper;
    private final RepairOrderImageMapper repairOrderImageMapper;
    private final RepairOrderFlowMapper repairOrderFlowMapper;
    private final SysUserMapper sysUserMapper;
    private final RepairProperties repairProperties;

    public RepairOrderServiceImpl(RepairOrderMapper repairOrderMapper,
                                  RepairOrderImageMapper repairOrderImageMapper,
                                  RepairOrderFlowMapper repairOrderFlowMapper,
                                  SysUserMapper sysUserMapper,
                                  RepairProperties repairProperties) {
        this.repairOrderMapper = repairOrderMapper;
        this.repairOrderImageMapper = repairOrderImageMapper;
        this.repairOrderFlowMapper = repairOrderFlowMapper;
        this.sysUserMapper = sysUserMapper;
        this.repairProperties = repairProperties;
    }

    @Override
    public RepairImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传图片文件");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("仅支持图片文件上传");
        }
        long maxBytes = repairProperties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片大小不能超过 " + repairProperties.getMaxFileSizeMb() + "MB");
        }

        String dateFolder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path targetDir = getUploadRoot().resolve(dateFolder).normalize();
        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            throw new BusinessException(500, "创建上传目录失败: " + ex.getMessage());
        }

        String suffix = fileSuffix(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        Path targetFile = targetDir.resolve(fileName).normalize();
        try {
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(500, "保存图片失败: " + ex.getMessage());
        }

        String relativePath = dateFolder + "/" + fileName;
        RepairImageUploadResponse response = new RepairImageUploadResponse();
        response.setPath(relativePath);
        response.setOriginalName(file.getOriginalFilename());
        response.setUrl("/api/repair/file?path=" + URLEncoder.encode(relativePath, StandardCharsets.UTF_8));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderDetailResponse createOrder(RepairOrderCreateRequest request, SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        RepairOrder order = new RepairOrder();
        order.setUserId(currentUser.getId());
        order.setTitle(request.getTitle());
        order.setDescription(request.getDescription());
        order.setContactPhone(StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone() : currentUser.getPhone());
        order.setStatus(RepairOrderStatus.SUBMITTED.name());
        order.setHandlerId(null);
        repairOrderMapper.insert(order);

        List<String> imagePaths = sanitizeImagePaths(request.getImagePaths());
        if (!imagePaths.isEmpty()) {
            repairOrderImageMapper.insertBatch(order.getId(), imagePaths);
        }

        RepairOrderFlow flow = new RepairOrderFlow();
        flow.setOrderId(order.getId());
        flow.setFromStatus("INIT");
        flow.setToStatus(RepairOrderStatus.SUBMITTED.name());
        flow.setRemark("用户提交报修");
        flow.setOperatorId(currentUser.getId());
        flow.setOperatorName(resolveUserName(currentUser));
        flow.setOperatorRole(currentUser.getRole());
        repairOrderFlowMapper.insert(flow);

        return buildDetailResponse(repairOrderMapper.selectById(order.getId()));
    }

    @Override
    public List<RepairOrderListItemResponse> listOrders(String status, SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        String normalizedStatus = normalizeStatus(status);
        List<RepairOrder> orders;
        if (ROLE_USER.equals(currentUser.getRole())) {
            orders = repairOrderMapper.selectByUserId(currentUser.getId(), normalizedStatus);
        } else {
            orders = repairOrderMapper.selectAll(normalizedStatus);
        }
        List<RepairOrderListItemResponse> list = new ArrayList<>();
        for (RepairOrder order : orders) {
            list.add(buildListItem(order));
        }
        return list;
    }

    @Override
    public RepairOrderDetailResponse getOrderDetail(Long id, SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }
        ensureReadable(order, currentUser);
        return buildDetailResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrderDetailResponse updateStatus(Long id, RepairOrderStatusUpdateRequest request, SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "请先登录");
        }
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "报修单不存在");
        }
        String targetStatus = normalizeStatus(request.getTargetStatus());
        String currentStatus = order.getStatus();
        if (Objects.equals(currentStatus, targetStatus)) {
            throw new BusinessException("状态未变化，无需重复提交");
        }
        if (!RepairOrderStatus.canTransition(currentStatus, targetStatus)) {
            throw new BusinessException("状态流转不合法: " + currentStatus + " -> " + targetStatus);
        }

        ensureStatusChangePermission(order, currentUser, targetStatus);

        Long handlerId = order.getHandlerId();
        if (RepairOrderStatus.ACCEPTED.name().equals(targetStatus)) {
            handlerId = currentUser.getId();
        }
        repairOrderMapper.updateStatus(order.getId(), targetStatus, handlerId);

        RepairOrderFlow flow = new RepairOrderFlow();
        flow.setOrderId(order.getId());
        flow.setFromStatus(currentStatus);
        flow.setToStatus(targetStatus);
        flow.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : defaultRemark(targetStatus));
        flow.setOperatorId(currentUser.getId());
        flow.setOperatorName(resolveUserName(currentUser));
        flow.setOperatorRole(currentUser.getRole());
        repairOrderFlowMapper.insert(flow);

        return buildDetailResponse(repairOrderMapper.selectById(order.getId()));
    }

    @Override
    public Resource loadImageAsResource(String path) {
        if (!StringUtils.hasText(path) || path.contains("..")) {
            throw new BusinessException(400, "非法图片路径");
        }
        Path target = getUploadRoot().resolve(path).normalize();
        if (!target.startsWith(getUploadRoot())) {
            throw new BusinessException(400, "非法图片路径");
        }
        if (!Files.exists(target) || !Files.isReadable(target)) {
            throw new BusinessException(404, "图片不存在");
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessException(500, "读取图片失败: " + ex.getMessage());
        }
    }

    private RepairOrderDetailResponse buildDetailResponse(RepairOrder order) {
        RepairOrderDetailResponse detail = new RepairOrderDetailResponse();
        fillBaseFields(detail, order);
        List<RepairOrderFlow> flows = repairOrderFlowMapper.selectByOrderId(order.getId());
        List<RepairOrderFlowResponse> flowResponses = flows.stream().map(this::toFlowResponse).collect(Collectors.toList());
        detail.setFlows(flowResponses);
        return detail;
    }

    private RepairOrderListItemResponse buildListItem(RepairOrder order) {
        RepairOrderListItemResponse response = new RepairOrderListItemResponse();
        fillBaseFields(response, order);
        return response;
    }

    private void fillBaseFields(RepairOrderListItemResponse response, RepairOrder order) {
        SysUser creator = sysUserMapper.selectById(order.getUserId());
        SysUser handler = order.getHandlerId() == null ? null : sysUserMapper.selectById(order.getHandlerId());
        List<RepairOrderImage> images = repairOrderImageMapper.selectByOrderId(order.getId());

        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setUsername(creator == null ? "-" : creator.getUsername());
        response.setUserNickname(creator == null ? "-" : creator.getNickname());
        response.setHandlerId(order.getHandlerId());
        response.setHandlerName(handler == null ? "-" : resolveUserName(handler));
        response.setTitle(order.getTitle());
        response.setDescription(order.getDescription());
        response.setContactPhone(order.getContactPhone());
        response.setStatus(order.getStatus());
        response.setStatusLabel(RepairOrderStatus.labelOf(order.getStatus()));
        response.setImagePaths(images.stream().map(RepairOrderImage::getImagePath).collect(Collectors.toList()));
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
    }

    private RepairOrderFlowResponse toFlowResponse(RepairOrderFlow flow) {
        RepairOrderFlowResponse response = new RepairOrderFlowResponse();
        response.setId(flow.getId());
        response.setFromStatus(flow.getFromStatus());
        response.setToStatus(flow.getToStatus());
        response.setFromStatusLabel("INIT".equals(flow.getFromStatus()) ? "初始化" : safeStatusLabel(flow.getFromStatus()));
        response.setToStatusLabel(safeStatusLabel(flow.getToStatus()));
        response.setRemark(flow.getRemark());
        response.setOperatorId(flow.getOperatorId());
        response.setOperatorName(flow.getOperatorName());
        response.setOperatorRole(flow.getOperatorRole());
        response.setCreatedAt(flow.getCreatedAt());
        return response;
    }

    private String safeStatusLabel(String code) {
        try {
            return RepairOrderStatus.labelOf(code);
        } catch (Exception ex) {
            return code;
        }
    }

    private List<String> sanitizeImagePaths(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return new ArrayList<>();
        }
        return imagePaths.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(item -> !item.contains(".."))
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        RepairOrderStatus.fromCode(normalized);
        return normalized;
    }

    private void ensureReadable(RepairOrder order, SysUser currentUser) {
        if (ROLE_USER.equals(currentUser.getRole()) && !order.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权查看该报修单");
        }
    }

    private void ensureStatusChangePermission(RepairOrder order, SysUser currentUser, String targetStatus) {
        if (RepairOrderStatus.isEmployeeActionTarget(targetStatus)) {
            if (!isAdminOrEmployee(currentUser)) {
                throw new BusinessException(403, "仅管理员或员工可执行该状态操作");
            }
            return;
        }
        if (RepairOrderStatus.isUserConfirmTarget(targetStatus)) {
            if (ROLE_ADMIN.equals(currentUser.getRole())) {
                return;
            }
            if (!order.getUserId().equals(currentUser.getId())) {
                throw new BusinessException(403, "仅报修发起人可确认维修完成");
            }
            return;
        }
        throw new BusinessException("不支持的状态操作: " + targetStatus + ", 可选: " + RepairOrderStatus.allowedCodes());
    }

    private boolean isAdminOrEmployee(SysUser user) {
        return ROLE_ADMIN.equals(user.getRole()) || ROLE_EMPLOYEE.equals(user.getRole());
    }

    private String resolveUserName(SysUser user) {
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    private String defaultRemark(String targetStatus) {
        return "状态变更为: " + RepairOrderStatus.labelOf(targetStatus);
    }

    private String fileSuffix(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return ".jpg";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return ".jpg";
        }
        String suffix = fileName.substring(idx).toLowerCase(Locale.ROOT);
        if (suffix.length() > 10) {
            return ".jpg";
        }
        return suffix;
    }

    private Path getUploadRoot() {
        Path root = Paths.get(repairProperties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new BusinessException(500, "创建上传根目录失败: " + ex.getMessage());
        }
        return root;
    }
}


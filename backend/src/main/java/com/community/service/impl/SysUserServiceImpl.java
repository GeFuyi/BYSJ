package com.community.service.impl;

import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.community.common.BusinessException;
import com.community.config.SmsLoginProperties;
import com.community.dto.LoginRequest;
import com.community.dto.LoginResponse;
import com.community.dto.RegisterRequest;
import com.community.dto.SmsCodeRequest;
import com.community.dto.SmsCodeSendResponse;
import com.community.dto.SmsLoginRequest;
import com.community.dto.UserRequest;
import com.community.dto.UserResponse;
import com.community.entity.SysUser;
import com.community.enums.UserRole;
import com.community.mapper.SysUserMapper;
import com.community.security.JwtUtil;
import com.community.security.PasswordUtil;
import com.community.service.SysUserService;
import com.community.sms.AliyunSmsCheckResult;
import com.community.sms.AliyunSmsGateway;
import com.community.sms.AliyunSmsSendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SysUserServiceImpl implements SysUserService {

    private static final String USER_KEY_PREFIX = "sys:user:";
    private static final String USER_LIST_KEY = "sys:user:list";
    private static final String SMS_INTERVAL_KEY_PREFIX = "sms:login:interval:";
    private static final String ALIYUN_OK = "OK";
    private static final String VERIFY_PASS = "PASS";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");
    private static final int MAX_AVATAR_PATH_LEN = 255;

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SmsLoginProperties smsLoginProperties;
    private final AliyunSmsGateway aliyunSmsGateway;

    public SysUserServiceImpl(SysUserMapper userMapper,
                              JwtUtil jwtUtil,
                              RedisTemplate<String, Object> redisTemplate,
                              ObjectMapper objectMapper,
                              SmsLoginProperties smsLoginProperties,
                              AliyunSmsGateway aliyunSmsGateway) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.smsLoginProperties = smsLoginProperties;
        this.aliyunSmsGateway = aliyunSmsGateway;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser register(RegisterRequest request) {
        if (!UserRole.isValid(request.getRole())) {
            throw new BusinessException("操作失败");
        }
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("操作失败");
        }

        String phone = defaultPhoneIfBlank(request.getPhone());
        assertPhoneValid(phone);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encrypt(request.getPassword()));
        user.setPhone(phone);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setAvatarPath(normalizeAvatarPath(request.getAvatarPath()));
        user.setRole(request.getRole());
        user.setStatus(1);

        userMapper.insert(user);
        SysUser saved = userMapper.selectById(user.getId());
        refreshUserCache(saved);
        evictUserListCache();
        return saved;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "请先登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "无权限访问");
        }
        return buildLoginResponse(user);
    }

    @Override
    public SmsCodeSendResponse sendSmsCode(SmsCodeRequest request) {
        String schemeName = resolveText(request.getSchemeName(), smsLoginProperties.getSchemeName());
        String countryCode = resolveText(request.getCountryCode(), smsLoginProperties.getDefaultCountryCode());
        String phone = resolveText(request.getPhoneNumber(), smsLoginProperties.getDefaultPhoneNumber());
        String outId = resolveText(request.getOutId(), UUID.randomUUID().toString().replace("-", ""));

        String signName = resolveText(request.getSignName(), smsLoginProperties.getSignName());
        String templateCode = resolveText(request.getTemplateCode(), smsLoginProperties.getTemplateCode());
        String templateParam = resolveText(request.getTemplateParam(), smsLoginProperties.getTemplateParam());
        String smsUpExtendCode = resolveText(request.getSmsUpExtendCode(), smsLoginProperties.getSmsUpExtendCode());

        long codeLength = resolveLong(request.getCodeLength(), smsLoginProperties.getCodeLength());
        long validTime = resolveLong(request.getValidTime(), smsLoginProperties.getValidTime());
        long duplicatePolicy = resolveLong(request.getDuplicatePolicy(), smsLoginProperties.getDuplicatePolicy());
        long intervalSeconds = resolveLong(request.getInterval(), smsLoginProperties.getIntervalSeconds());
        long codeType = resolveLong(request.getCodeType(), smsLoginProperties.getCodeType());
        boolean returnVerifyCode = resolveBoolean(request.getReturnVerifyCode(), smsLoginProperties.isReturnVerifyCode());

        assertPhoneValid(phone);

        if (!StringUtils.hasText(signName) || !StringUtils.hasText(templateCode)) {
            throw new BusinessException("操作失败");
        }

        String intervalKey = smsIntervalKey(countryCode, phone);
        if (intervalSeconds > 0 && Boolean.TRUE.equals(redisTemplate.hasKey(intervalKey))) {
            throw new BusinessException("操作失败");
        }

        SendSmsVerifyCodeRequest.Builder builder = SendSmsVerifyCodeRequest.builder()
                .schemeName(schemeName)
                .countryCode(countryCode)
                .phoneNumber(phone)
                .signName(signName)
                .templateCode(templateCode)
                .templateParam(templateParam)
                .outId(outId)
                .codeLength(codeLength)
                .validTime(validTime)
                .duplicatePolicy(duplicatePolicy)
                .interval(intervalSeconds)
                .codeType(codeType)
                .returnVerifyCode(returnVerifyCode);

        if (StringUtils.hasText(smsUpExtendCode)) {
            builder.smsUpExtendCode(smsUpExtendCode);
        }

        AliyunSmsSendResult sendResult = aliyunSmsGateway.sendSmsVerifyCode(builder.build());
        if (!ALIYUN_OK.equalsIgnoreCase(sendResult.getCode()) || !Boolean.TRUE.equals(sendResult.getSuccess())) {
            throw new BusinessException(502, "第三方服务调用失败");
        }

        if (intervalSeconds > 0) {
            redisTemplate.opsForValue().set(intervalKey, "1", Duration.ofSeconds(intervalSeconds));
        }

        SmsCodeSendResponse response = new SmsCodeSendResponse();
        response.setSchemeName(schemeName);
        response.setCountryCode(countryCode);
        response.setPhoneNumber(phone);
        response.setOutId(StringUtils.hasText(sendResult.getOutId()) ? sendResult.getOutId() : outId);
        response.setCodeLength((int) codeLength);
        response.setValidMinutes((int) Math.max(1, validTime / 60));
        response.setVerifyCode(sendResult.getVerifyCode());
        response.setBizId(sendResult.getBizId());
        response.setRequestId(sendResult.getRequestId());
        response.setProviderCode(sendResult.getCode());
        response.setProviderMessage(sendResult.getMessage());
        return response;
    }

    @Override
    public LoginResponse smsLogin(SmsLoginRequest request) {
        String schemeName = resolveText(request.getSchemeName(), smsLoginProperties.getSchemeName());
        String countryCode = resolveText(request.getCountryCode(), smsLoginProperties.getDefaultCountryCode());
        String phone = request.getPhoneNumber();
        assertPhoneValid(phone);

        long caseAuthPolicy = resolveCaseAuthPolicy(request.getCaseAuthPolicy());

        CheckSmsVerifyCodeRequest.Builder checkRequestBuilder = CheckSmsVerifyCodeRequest.builder()
                .schemeName(schemeName)
                .countryCode(countryCode)
                .phoneNumber(phone)
                .verifyCode(request.getVerifyCode())
                .caseAuthPolicy(caseAuthPolicy);
        if (StringUtils.hasText(request.getOutId())) {
            checkRequestBuilder.outId(request.getOutId());
        }

        AliyunSmsCheckResult checkResult = aliyunSmsGateway.checkSmsVerifyCode(checkRequestBuilder.build());
        if (!ALIYUN_OK.equalsIgnoreCase(checkResult.getCode()) || !Boolean.TRUE.equals(checkResult.getSuccess())) {
            throw new BusinessException(400, "请求参数不合法");
        }
        if (!VERIFY_PASS.equalsIgnoreCase(checkResult.getVerifyResult())) {
            throw new BusinessException(400, "请求参数不合法");
        }

        SysUser user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "无权限访问");
        }
        return buildLoginResponse(user);
    }

    @Override
    public List<SysUser> listUsers() {
        Object cached = redisTemplate.opsForValue().get(USER_LIST_KEY);
        if (cached != null) {
            return convertToUserList(cached);
        }
        List<SysUser> users = userMapper.selectAll();
        redisTemplate.opsForValue().set(USER_LIST_KEY, users, Duration.ofMinutes(10));
        return users;
    }

    @Override
    public SysUser getUserById(Long id) {
        Object cached = redisTemplate.opsForValue().get(userKey(id));
        if (cached != null) {
            return objectMapper.convertValue(cached, SysUser.class);
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("操作失败");
        }
        refreshUserCache(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(UserRequest request) {
        validateCreateOrUpdateRequest(request, true);
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BusinessException("操作失败");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encrypt(request.getPassword()));
        user.setPhone(defaultPhoneIfBlank(request.getPhone()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setAvatarPath(normalizeAvatarPath(request.getAvatarPath()));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        userMapper.insert(user);
        SysUser saved = userMapper.selectById(user.getId());
        refreshUserCache(saved);
        evictUserListCache();
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(Long id, UserRequest request) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("操作失败");
        }
        validateCreateOrUpdateRequest(request, false);

        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(existing.getUsername())) {
            SysUser target = userMapper.selectByUsername(request.getUsername());
            if (target != null && !target.getId().equals(id)) {
                throw new BusinessException("操作失败");
            }
            existing.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getPassword())) {
            existing.setPassword(PasswordUtil.encrypt(request.getPassword()));
        }
        if (StringUtils.hasText(request.getPhone())) {
            existing.setPhone(request.getPhone());
        }
        if (request.getNickname() != null) {
            existing.setNickname(request.getNickname());
        }
        if (request.getAvatarPath() != null) {
            existing.setAvatarPath(normalizeAvatarPath(request.getAvatarPath()));
        }
        if (StringUtils.hasText(request.getRole())) {
            existing.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        userMapper.update(existing);
        SysUser saved = userMapper.selectById(id);
        refreshUserCache(saved);
        evictUserListCache();
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("操作失败");
        }
        userMapper.deleteById(id);
        redisTemplate.delete(userKey(id));
        evictUserListCache();
    }

    @Override
    public SysUser getByUsername(String username) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(404, "资源不存在");
        }
        return user;
    }

    @Override
    public SysUser getByPhone(String phone) {
        SysUser user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new BusinessException(404, "资源不存在");
        }
        return user;
    }

    private void validateCreateOrUpdateRequest(UserRequest request, boolean create) {
        if (create && !StringUtils.hasText(request.getUsername())) {
            throw new BusinessException("操作失败");
        }
        if (create && !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("操作失败");
        }
        if (create && !StringUtils.hasText(request.getRole())) {
            throw new BusinessException("操作失败");
        }
        if (StringUtils.hasText(request.getRole()) && !UserRole.isValid(request.getRole())) {
            throw new BusinessException("操作失败");
        }
        if (StringUtils.hasText(request.getPhone())) {
            assertPhoneValid(request.getPhone());
        }
        if (request.getAvatarPath() != null && request.getAvatarPath().length() > MAX_AVATAR_PATH_LEN) {
            throw new BusinessException("操作失败");
        }
    }

    private LoginResponse buildLoginResponse(SysUser user) {
        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user.getUsername(), user.getRole()));
        response.setUser(UserResponse.from(user));
        return response;
    }

    private long resolveCaseAuthPolicy(String caseAuthPolicy) {
        if (!StringUtils.hasText(caseAuthPolicy)) {
            return smsLoginProperties.getCaseAuthPolicy();
        }

        if (caseAuthPolicy.matches("^\\d+$")) {
            long value = Long.parseLong(caseAuthPolicy);
            if (value == 1 || value == 2) {
                return value;
            }
            throw new BusinessException("操作失败");
        }

        String upperValue = caseAuthPolicy.trim().toUpperCase();
        if ("IGNORE_CASE".equals(upperValue) || "CASE_INSENSITIVE".equals(upperValue)) {
            return 1;
        }
        if ("STRICT".equals(upperValue) || "CASE_SENSITIVE".equals(upperValue)) {
            return 2;
        }
        throw new BusinessException("操作失败");
    }

    private long resolveLong(Long requestValue, long defaultValue) {
        return requestValue == null ? defaultValue : requestValue;
    }

    private boolean resolveBoolean(Boolean requestValue, boolean defaultValue) {
        return requestValue == null ? defaultValue : requestValue;
    }

    private String resolveText(String requestValue, String defaultValue) {
        return StringUtils.hasText(requestValue) ? requestValue : defaultValue;
    }

    private void assertPhoneValid(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("操作失败");
        }
    }

    private String defaultPhoneIfBlank(String phone) {
        return StringUtils.hasText(phone) ? phone : smsLoginProperties.getDefaultPhoneNumber();
    }

    private String normalizeAvatarPath(String avatarPath) {
        if (!StringUtils.hasText(avatarPath)) {
            return null;
        }
        String normalized = avatarPath.trim();
        if (normalized.length() > MAX_AVATAR_PATH_LEN) {
            throw new BusinessException("操作失败");
        }
        if (normalized.contains("..")) {
            throw new BusinessException("操作失败");
        }
        return normalized;
    }

    private List<SysUser> convertToUserList(Object cached) {
        List<SysUser> users = new ArrayList<>();
        if (cached instanceof List<?>) {
            List<?> rawList = (List<?>) cached;
            for (Object obj : rawList) {
                users.add(objectMapper.convertValue(obj, SysUser.class));
            }
        }
        return users;
    }

    private void refreshUserCache(SysUser user) {
        redisTemplate.opsForValue().set(userKey(user.getId()), user, Duration.ofHours(6));
    }

    private void evictUserListCache() {
        redisTemplate.delete(USER_LIST_KEY);
    }

    private String userKey(Long id) {
        return USER_KEY_PREFIX + id;
    }

    private String smsIntervalKey(String countryCode, String phone) {
        return SMS_INTERVAL_KEY_PREFIX + countryCode + ":" + phone;
    }
}

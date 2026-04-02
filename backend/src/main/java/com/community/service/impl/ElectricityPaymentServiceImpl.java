package com.community.service.impl;

import com.alipay.api.domain.ChargeInstMode;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.response.AlipayEbppBillAddResponse;
import com.alipay.api.response.AlipayEbppBillGetResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.community.common.BusinessException;
import com.community.config.AlipayProperties;
import com.community.dto.ElectricityDefaultsResponse;
import com.community.dto.ElectricityOrderCreateRequest;
import com.community.dto.ElectricityOrderResponse;
import com.community.entity.ElectricityPaymentOrder;
import com.community.entity.SysUser;
import com.community.enums.ElectricityOrderStatus;
import com.community.mapper.ElectricityPaymentOrderMapper;
import com.community.payment.AlipayGateway;
import com.community.payment.QrCodeService;
import com.community.service.ElectricityPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ElectricityPaymentServiceImpl implements ElectricityPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ElectricityPaymentServiceImpl.class);
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final AlipayProperties properties;
    private final ElectricityPaymentOrderMapper orderMapper;
    private final AlipayGateway alipayGateway;
    private final QrCodeService qrCodeService;

    public ElectricityPaymentServiceImpl(AlipayProperties properties,
                                         ElectricityPaymentOrderMapper orderMapper,
                                         AlipayGateway alipayGateway,
                                         QrCodeService qrCodeService) {
        this.properties = properties;
        this.orderMapper = orderMapper;
        this.alipayGateway = alipayGateway;
        this.qrCodeService = qrCodeService;
    }

    @Override
    public ElectricityDefaultsResponse getDefaults() {
        ElectricityDefaultsResponse response = new ElectricityDefaultsResponse();
        response.setDefaultChargeInst(properties.getDefaultChargeInst());
        response.setDefaultBillKey(properties.getDefaultBillKey());
        response.setDefaultOwnerName(properties.getDefaultOwnerName());
        response.setDefaultOrderType(properties.getDefaultOrderType());
        response.setDefaultSubOrderType(properties.getDefaultSubOrderType());
        response.setAlipayEnabled(properties.isEnabled());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElectricityOrderResponse createOrder(ElectricityOrderCreateRequest request, SysUser currentUser) {
        requireResidentRole(currentUser);
        alipayGateway.ensureEnabled();

        String chargeInst = resolveText(request.getChargeInst(), properties.getDefaultChargeInst());
        String billKey = resolveText(request.getBillKey(), properties.getDefaultBillKey());
        String ownerName = resolveText(request.getOwnerName(), resolveDefaultOwner(currentUser));
        BigDecimal payAmount = request.getPayAmount().setScale(2, RoundingMode.HALF_UP);
        if (!StringUtils.hasText(chargeInst)) {
            throw new BusinessException("缴费机构不能为空");
        }
        if (!StringUtils.hasText(billKey)) {
            throw new BusinessException("户号不能为空");
        }
        String chargeInstCode = normalizeChargeInstCode(chargeInst, properties.getDefaultOrderType(), properties.getDefaultSubOrderType());

        String merchantOrderNo = generateOrderNo("EBPP");
        String outTradeNo = generateOrderNo("PAY");
        ElectricityPaymentOrder order = new ElectricityPaymentOrder();
        order.setUserId(currentUser.getId());
        order.setMerchantOrderNo(merchantOrderNo);
        order.setOutTradeNo(outTradeNo);
        order.setChargeInst(chargeInst);
        order.setBillKey(billKey);
        order.setOwnerName(ownerName);
        order.setOrderType(properties.getDefaultOrderType());
        order.setSubOrderType(properties.getDefaultSubOrderType());
        order.setPayAmount(payAmount);
        order.setServiceAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(ElectricityOrderStatus.CREATED.name());
        order.setRawMessage("订单已创建，等待调用支付宝接口");
        orderMapper.insert(order);

        try {
            AlipayEbppBillAddResponse billAddResponse = alipayGateway.addElectricityBill(
                    merchantOrderNo,
                    chargeInstCode,
                    billKey,
                    ownerName,
                    payAmount,
                    order.getOrderType(),
                    order.getSubOrderType(),
                    currentUser.getPhone()
            );

            AlipayTradePrecreateResponse precreateResponse = alipayGateway.precreateOrder(
                    outTradeNo,
                    payAmount,
                    buildSubject(billKey),
                    buildBody(chargeInst, billKey, merchantOrderNo)
            );

            order.setEbppAlipayOrderNo(billAddResponse.getAlipayOrderNo());
            order.setEbppOrderStatus("CREATED");
            order.setQrCode(precreateResponse.getQrCode());
            order.setStatus(ElectricityOrderStatus.WAIT_PAY.name());
            order.setRawMessage("订单创建成功，等待支付");
            orderMapper.updateCreateResult(order);
            return toResponse(orderMapper.selectById(order.getId()));
        } catch (BusinessException ex) {
            order.setStatus(ElectricityOrderStatus.FAILED.name());
            order.setRawMessage(ex.getMessage());
            orderMapper.updateCreateResult(order);
            throw ex;
        }
    }

    @Override
    public List<ElectricityOrderResponse> myOrders(SysUser currentUser) {
        requireResidentRole(currentUser);
        List<ElectricityPaymentOrder> orders = orderMapper.selectByUserId(currentUser.getId());
        List<ElectricityOrderResponse> responses = new ArrayList<>();
        for (ElectricityPaymentOrder order : orders) {
            responses.add(toResponse(order));
        }
        return responses;
    }

    @Override
    public ElectricityOrderResponse getOrder(Long id, SysUser currentUser) {
        requireResidentRole(currentUser);
        ElectricityPaymentOrder order = loadOrderAndCheckReadable(id, currentUser);
        return toResponse(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ElectricityOrderResponse refreshOrderStatus(Long id, SysUser currentUser) {
        requireResidentRole(currentUser);
        ElectricityPaymentOrder order = loadOrderAndCheckReadable(id, currentUser);
        String currentStatus = order.getStatus();
        if (ElectricityOrderStatus.PAID.name().equals(currentStatus)
                || ElectricityOrderStatus.CLOSED.name().equals(currentStatus)) {
            return toResponse(order);
        }

        AlipayTradeQueryResponse queryResponse = alipayGateway.queryTrade(order.getOutTradeNo());
        String tradeStatus = queryResponse.getTradeStatus();
        String ebppOrderStatus = fetchEbppStatus(order);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderMapper.updatePaid(order.getId(),
                    StringUtils.hasText(queryResponse.getTradeNo()) ? queryResponse.getTradeNo() : order.getTradeNo(),
                    ebppOrderStatus,
                    new Date());
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            orderMapper.updateClosed(order.getId(), "交易已关闭", ebppOrderStatus);
        } else if (StringUtils.hasText(ebppOrderStatus)) {
            orderMapper.updateEbppStatus(order.getId(), ebppOrderStatus);
        }
        return toResponse(orderMapper.selectById(order.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return "failure";
        }
        Map<String, String> params = extractRequestParams(request);
        try {
            boolean signPass = AlipaySignature.rsaCheckV1(
                    params,
                    properties.getAlipayPublicKey(),
                    properties.getCharset(),
                    properties.getSignType()
            );
            if (!signPass) {
                return "failure";
            }
        } catch (AlipayApiException ex) {
            log.warn("alipay notify verify failed: {}", ex.getErrMsg());
            return "failure";
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String tradeNo = params.get("trade_no");
        if (!StringUtils.hasText(outTradeNo) || !StringUtils.hasText(tradeStatus)) {
            return "failure";
        }
        ElectricityPaymentOrder order = orderMapper.selectByOutTradeNo(outTradeNo);
        if (order == null) {
            return "success";
        }
        String ebppOrderStatus = fetchEbppStatus(order);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            orderMapper.updatePaid(order.getId(),
                    StringUtils.hasText(tradeNo) ? tradeNo : order.getTradeNo(),
                    ebppOrderStatus,
                    new Date());
        } else if ("TRADE_CLOSED".equals(tradeStatus)) {
            orderMapper.updateClosed(order.getId(), "交易已关闭", ebppOrderStatus);
        }
        return "success";
    }

    private ElectricityOrderResponse toResponse(ElectricityPaymentOrder order) {
        String qrCodeImage = null;
        if (StringUtils.hasText(order.getQrCode())
                && ElectricityOrderStatus.WAIT_PAY.name().equals(order.getStatus())) {
            qrCodeImage = qrCodeService.toDataUrl(order.getQrCode(), 260);
        }
        return ElectricityOrderResponse.from(order, qrCodeImage);
    }

    private ElectricityPaymentOrder loadOrderAndCheckReadable(Long id, SysUser currentUser) {
        ElectricityPaymentOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "资源不存在");
        }
        if (!ROLE_ADMIN.equals(currentUser.getRole()) && !order.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(403, "无权限访问");
        }
        return order;
    }

    private void requireResidentRole(SysUser user) {
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!ROLE_USER.equals(user.getRole()) && !ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    private String fetchEbppStatus(ElectricityPaymentOrder order) {
        try {
            AlipayEbppBillGetResponse billGetResponse = alipayGateway.getBill(order.getMerchantOrderNo(), order.getOrderType());
            return billGetResponse.getOrderStatus();
        } catch (Exception ex) {
            log.warn("fetch ebpp status failed, merchantOrderNo={}, error={}", order.getMerchantOrderNo(), ex.getMessage());
            return order.getEbppOrderStatus();
        }
    }

    private String buildSubject(String billKey) {
        String suffix = billKey;
        if (StringUtils.hasText(billKey) && billKey.length() > 6) {
            suffix = billKey.substring(billKey.length() - 6);
        }
        return properties.getSubjectPrefix() + "户号尾号" + suffix;
    }

    private String buildBody(String chargeInst, String billKey, String merchantOrderNo) {
        return "chargeInst=" + chargeInst + ",billKey=" + billKey + ",merchantOrderNo=" + merchantOrderNo;
    }

    private String normalizeChargeInstCode(String chargeInst, String orderType, String subOrderType) {
        if (!StringUtils.hasText(chargeInst)) {
            return chargeInst;
        }
        if (!containsChinese(chargeInst)) {
            return chargeInst;
        }
        try {
            List<ChargeInstMode> list = alipayGateway.searchChargeInst(
                    properties.getDefaultProvince(),
                    properties.getDefaultCity(),
                    orderType,
                    subOrderType
            );
            for (ChargeInstMode item : list) {
                if (!StringUtils.hasText(item.getChargeInst()) || !StringUtils.hasText(item.getChargeInstName())) {
                    continue;
                }
                String name = item.getChargeInstName();
                if (name.equalsIgnoreCase(chargeInst) || name.contains(chargeInst) || chargeInst.contains(name)) {
                    return item.getChargeInst();
                }
            }
            log.warn("chargeInst name -> code mapping not found, use original input: {}", chargeInst);
            return chargeInst;
        } catch (Exception ex) {
            log.warn("chargeInst name -> code mapping failed, use original input: {}, error={}", chargeInst, ex.getMessage());
            return chargeInst;
        }
    }

    private String resolveDefaultOwner(SysUser user) {
        if (user == null) {
            return properties.getDefaultOwnerName();
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername();
        }
        return properties.getDefaultOwnerName();
    }

    private String resolveText(String input, String fallback) {
        if (StringUtils.hasText(input)) {
            return input.trim();
        }
        return fallback;
    }

    private String generateOrderNo(String prefix) {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return prefix + timestamp + random;
    }

    private boolean containsChinese(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> extractRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        return params;
    }
}

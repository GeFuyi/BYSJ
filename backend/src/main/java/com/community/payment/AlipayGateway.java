package com.community.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayRequest;
import com.alipay.api.AlipayResponse;
import com.alipay.api.domain.ChargeInstMode;
import com.alipay.api.request.AlipayEbppBillAddRequest;
import com.alipay.api.request.AlipayEbppBillGetRequest;
import com.alipay.api.request.AlipayEbppConfigChargeinstSearchRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayEbppBillAddResponse;
import com.alipay.api.response.AlipayEbppBillGetResponse;
import com.alipay.api.response.AlipayEbppConfigChargeinstSearchResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.community.common.BusinessException;
import com.community.config.AlipayProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class AlipayGateway {

    private final AlipayProperties properties;
    private volatile AlipayClient alipayClient;

    public AlipayGateway(AlipayProperties properties) {
        this.properties = properties;
    }

    public AlipayEbppBillAddResponse addElectricityBill(String merchantOrderNo,
                                                         String chargeInst,
                                                         String billKey,
                                                         String ownerName,
                                                         BigDecimal amount,
                                                         String orderType,
                                                         String subOrderType,
                                                         String mobile) {
        AlipayEbppBillAddRequest request = new AlipayEbppBillAddRequest();
        request.setMerchantOrderNo(merchantOrderNo);
        request.setChargeInst(chargeInst);
        request.setBillKey(billKey);
        request.setOwnerName(ownerName);
        request.setPayAmount(amount.toPlainString());
        request.setServiceAmount("0");
        request.setOrderType(orderType);
        request.setSubOrderType(subOrderType);
        request.setBillDate(new java.text.SimpleDateFormat("yyyyMM").format(new java.util.Date()));
        if (StringUtils.hasText(mobile)) {
            request.setMobile(mobile);
        }
        try {
            AlipayEbppBillAddResponse response = execute(request);
            if (response == null) {
                throw new BusinessException(502, "支付宝服务调用失败");
            }
            if (!response.isSuccess()) {
                throw new BusinessException(502, "支付宝生活缴费下单失败，错误码：" + response.getSubCode());
            }
            return response;
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "支付宝服务调用失败");
        }
    }

    public AlipayEbppBillGetResponse getBill(String merchantOrderNo, String orderType) {
        AlipayEbppBillGetRequest request = new AlipayEbppBillGetRequest();
        request.setMerchantOrderNo(merchantOrderNo);
        request.setOrderType(orderType);
        try {
            AlipayEbppBillGetResponse response = execute(request);
            if (response == null) {
                throw new BusinessException(502, "支付宝服务调用失败");
            }
            if (!response.isSuccess()) {
                throw new BusinessException(502, "支付宝生活缴费查询失败，错误码：" + response.getSubCode());
            }
            return response;
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "支付宝服务调用失败");
        }
    }

    public AlipayTradePrecreateResponse precreateOrder(String outTradeNo, BigDecimal totalAmount, String subject, String body) {
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        com.alipay.api.domain.AlipayTradePrecreateModel model = new com.alipay.api.domain.AlipayTradePrecreateModel();
        model.setOutTradeNo(outTradeNo);
        model.setTotalAmount(totalAmount.toPlainString());
        model.setSubject(subject);
        model.setBody(body);
        model.setTimeoutExpress(properties.getTimeoutExpress());
        request.setBizModel(model);
        if (StringUtils.hasText(properties.getNotifyUrl())) {
            request.setNotifyUrl(properties.getNotifyUrl());
        }
        try {
            AlipayTradePrecreateResponse response = execute(request);
            if (response == null) {
                throw new BusinessException(502, "支付宝服务调用失败");
            }
            if (!response.isSuccess() || !StringUtils.hasText(response.getQrCode())) {
                throw new BusinessException(502, "支付宝预下单失败，错误码：" + response.getSubCode());
            }
            return response;
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "支付宝服务调用失败");
        }
    }

    public AlipayTradeQueryResponse queryTrade(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        com.alipay.api.domain.AlipayTradeQueryModel model = new com.alipay.api.domain.AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = execute(request);
            if (response == null) {
                throw new BusinessException(502, "支付宝服务调用失败");
            }
            if (!response.isSuccess()) {
                throw new BusinessException(502, "支付宝交易查询失败，错误码：" + response.getSubCode());
            }
            return response;
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "支付宝服务调用失败");
        }
    }

    public List<ChargeInstMode> searchChargeInst(String province, String city, String orderType, String subOrderType) {
        AlipayEbppConfigChargeinstSearchRequest request = new AlipayEbppConfigChargeinstSearchRequest();
        request.setProvince(province);
        request.setCity(city);
        request.setOrderType(orderType);
        request.setSubOrderType(subOrderType);
        try {
            AlipayEbppConfigChargeinstSearchResponse response = execute(request);
            if (response == null) {
                throw new BusinessException(502, "支付宝服务调用失败");
            }
            if (!response.isSuccess()) {
                throw new BusinessException(502, "支付宝缴费机构查询失败，错误码：" + response.getSubCode());
            }
            List<ChargeInstMode> list = response.getChargeInstModeResult();
            return list == null ? new ArrayList<ChargeInstMode>() : list;
        } catch (AlipayApiException ex) {
            throw new BusinessException(502, "支付宝服务调用失败");
        }
    }

    private <T extends AlipayResponse> T execute(AlipayRequest<T> request) throws AlipayApiException {
        String accessToken = normalizeToken(properties.getAccessToken());
        String appAuthToken = normalizeToken(properties.getAppAuthToken());
        if (StringUtils.hasText(accessToken) || StringUtils.hasText(appAuthToken)) {
            return getClient().execute(request, accessToken, appAuthToken);
        }
        return getClient().execute(request);
    }

    private String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return token.trim();
    }

    private AlipayClient getClient() {
        ensureEnabled();
        if (alipayClient != null) {
            return alipayClient;
        }
        synchronized (this) {
            if (alipayClient == null) {
                validateBaseConfig();
                alipayClient = new DefaultAlipayClient(
                        properties.getGatewayUrl(),
                        properties.getAppId(),
                        properties.getPrivateKey(),
                        properties.getFormat(),
                        properties.getCharset(),
                        properties.getAlipayPublicKey(),
                        properties.getSignType()
                );
            }
        }
        return alipayClient;
    }

    public void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(400, "支付宝缴费功能未启用");
        }
    }

    private void validateBaseConfig() {
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getPrivateKey())
                || !StringUtils.hasText(properties.getAlipayPublicKey())) {
            throw new BusinessException(500, "支付宝配置不完整，请联系管理员");
        }
    }
}

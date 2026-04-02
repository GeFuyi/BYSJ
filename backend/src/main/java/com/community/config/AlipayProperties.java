package com.community.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    private boolean enabled = false;
    private String gatewayUrl = "https://openapi.alipay.com/gateway.do";
    private String appId;
    private String privateKey;
    private String alipayPublicKey;
    private String accessToken;
    private String appAuthToken;
    private String charset = "UTF-8";
    private String signType = "RSA2";
    private String format = "json";
    private String notifyUrl;
    private String defaultOrderType = "JF";
    private String defaultSubOrderType = "ELEC";
    private String defaultChargeInst = "国家电网";
    private String defaultProvince = "河南省";
    private String defaultCity = "郑州市";
    private String defaultBillKey = "4107880059197";
    private String defaultOwnerName = "社区住户";
    private String subjectPrefix = "社区电费代缴-";
    private String timeoutExpress = "15m";
}

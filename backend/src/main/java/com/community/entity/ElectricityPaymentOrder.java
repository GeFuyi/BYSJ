package com.community.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ElectricityPaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String merchantOrderNo;
    private String outTradeNo;
    private String tradeNo;
    private String ebppAlipayOrderNo;
    private String ebppOrderStatus;
    private String chargeInst;
    private String billKey;
    private String ownerName;
    private String orderType;
    private String subOrderType;
    private BigDecimal payAmount;
    private BigDecimal serviceAmount;
    private String qrCode;
    private String status;
    private String rawMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date paidAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;
}


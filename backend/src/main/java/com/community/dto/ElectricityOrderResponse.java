package com.community.dto;

import com.community.entity.ElectricityPaymentOrder;
import com.community.enums.ElectricityOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ElectricityOrderResponse {

    private Long id;
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
    private String qrCodeImage;
    private String status;
    private String statusLabel;
    private String rawMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date paidAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

    public static ElectricityOrderResponse from(ElectricityPaymentOrder order, String qrCodeImage) {
        ElectricityOrderResponse response = new ElectricityOrderResponse();
        response.setId(order.getId());
        response.setMerchantOrderNo(order.getMerchantOrderNo());
        response.setOutTradeNo(order.getOutTradeNo());
        response.setTradeNo(order.getTradeNo());
        response.setEbppAlipayOrderNo(order.getEbppAlipayOrderNo());
        response.setEbppOrderStatus(order.getEbppOrderStatus());
        response.setChargeInst(order.getChargeInst());
        response.setBillKey(order.getBillKey());
        response.setOwnerName(order.getOwnerName());
        response.setOrderType(order.getOrderType());
        response.setSubOrderType(order.getSubOrderType());
        response.setPayAmount(order.getPayAmount());
        response.setServiceAmount(order.getServiceAmount());
        response.setQrCode(order.getQrCode());
        response.setQrCodeImage(qrCodeImage);
        response.setStatus(order.getStatus());
        response.setStatusLabel(toStatusLabel(order.getStatus()));
        response.setRawMessage(order.getRawMessage());
        response.setPaidAt(order.getPaidAt());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    private static String toStatusLabel(String statusCode) {
        if (statusCode == null) {
            return "-";
        }
        try {
            return ElectricityOrderStatus.fromCode(statusCode).getLabel();
        } catch (Exception ex) {
            return statusCode;
        }
    }
}


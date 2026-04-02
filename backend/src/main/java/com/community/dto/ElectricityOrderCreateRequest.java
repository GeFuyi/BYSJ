package com.community.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Data
public class ElectricityOrderCreateRequest {

    @Size(max = 64, message = "缴费机构长度不能超过64个字符")
    private String chargeInst;

    @Size(max = 64, message = "户号长度不能超过64个字符")
    private String billKey;

    @Size(max = 50, message = "户主姓名长度不能超过50个字符")
    private String ownerName;

    @NotNull(message = "缴费金额不能为空")
    @DecimalMin(value = "0.01", message = "缴费金额必须大于0")
    private BigDecimal payAmount;
}


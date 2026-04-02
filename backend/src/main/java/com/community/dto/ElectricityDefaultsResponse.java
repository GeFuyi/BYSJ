package com.community.dto;

import lombok.Data;

@Data
public class ElectricityDefaultsResponse {

    private String defaultChargeInst;
    private String defaultBillKey;
    private String defaultOwnerName;
    private String defaultOrderType;
    private String defaultSubOrderType;
    private boolean alipayEnabled;
}


package com.community.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepairOrderDetailResponse extends RepairOrderListItemResponse {

    private List<RepairOrderFlowResponse> flows;
}


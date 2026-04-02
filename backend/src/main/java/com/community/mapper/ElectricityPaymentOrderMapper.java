package com.community.mapper;

import com.community.entity.ElectricityPaymentOrder;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ElectricityPaymentOrderMapper {

    int insert(ElectricityPaymentOrder order);

    ElectricityPaymentOrder selectById(@Param("id") Long id);

    ElectricityPaymentOrder selectByMerchantOrderNo(@Param("merchantOrderNo") String merchantOrderNo);

    ElectricityPaymentOrder selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    List<ElectricityPaymentOrder> selectByUserId(@Param("userId") Long userId);

    int updateCreateResult(ElectricityPaymentOrder order);

    int updatePaid(@Param("id") Long id,
                   @Param("tradeNo") String tradeNo,
                   @Param("ebppOrderStatus") String ebppOrderStatus,
                   @Param("paidAt") Date paidAt);

    int updateClosed(@Param("id") Long id,
                     @Param("rawMessage") String rawMessage,
                     @Param("ebppOrderStatus") String ebppOrderStatus);

    int updateEbppStatus(@Param("id") Long id, @Param("ebppOrderStatus") String ebppOrderStatus);
}


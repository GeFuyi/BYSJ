package com.community.mapper;

import com.community.entity.ConvenienceService;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConvenienceServiceMapper {

    int insert(ConvenienceService service);

    int updateForResubmit(ConvenienceService service);

    int updateOperateStatus(@Param("id") Long id, @Param("serviceStatus") String serviceStatus);

    int updateAuditResult(@Param("id") Long id,
                          @Param("auditStatus") String auditStatus,
                          @Param("auditReason") String auditReason,
                          @Param("reviewedBy") Long reviewedBy);

    int incrementBookedIfAvailable(@Param("id") Long id);

    int refreshScore(@Param("id") Long id);

    ConvenienceService selectById(@Param("id") Long id);

    List<ConvenienceService> selectProviderList(@Param("providerId") Long providerId,
                                                @Param("auditStatus") String auditStatus);

    List<ConvenienceService> selectAuditList(@Param("auditStatus") String auditStatus,
                                             @Param("keyword") String keyword);

    List<ConvenienceService> selectPublishedList(@Param("keyword") String keyword,
                                                 @Param("categoryCode") String categoryCode,
                                                 @Param("serviceStatus") String serviceStatus);
}


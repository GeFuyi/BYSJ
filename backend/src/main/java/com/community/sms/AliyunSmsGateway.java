package com.community.sms;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.EnvironmentVariableCredentialProvider;
import com.aliyun.auth.credentials.provider.ICredentialProvider;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.community.common.BusinessException;
import com.community.config.SmsLoginProperties;
import darabonba.core.client.ClientOverrideConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;

@Component
public class AliyunSmsGateway {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsGateway.class);

    private final SmsLoginProperties properties;
    private volatile AsyncClient client;

    public AliyunSmsGateway(SmsLoginProperties properties) {
        this.properties = properties;
    }

    public AliyunSmsSendResult sendSmsVerifyCode(SendSmsVerifyCodeRequest request) {
        try {
            SendSmsVerifyCodeResponse response = getClient().sendSmsVerifyCode(request).get();
            SendSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null) {
                throw new BusinessException(502, "阿里云短信发送失败：响应体为空");
            }
            SendSmsVerifyCodeResponseBody.Model model = body.getModel();
            return new AliyunSmsSendResult(
                    body.getCode(),
                    body.getMessage(),
                    body.getSuccess(),
                    model == null ? null : model.getOutId(),
                    model == null ? null : model.getBizId(),
                    model == null ? null : model.getRequestId(),
                    model == null ? null : model.getVerifyCode()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "短信发送被中断");
        } catch (ExecutionException ex) {
            throw wrapException("短信发送", ex.getCause() == null ? ex : ex.getCause());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrapException("短信发送", ex);
        }
    }

    public AliyunSmsCheckResult checkSmsVerifyCode(CheckSmsVerifyCodeRequest request) {
        try {
            CheckSmsVerifyCodeResponse response = getClient().checkSmsVerifyCode(request).get();
            CheckSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null) {
                throw new BusinessException(502, "阿里云验证码校验失败：响应体为空");
            }
            CheckSmsVerifyCodeResponseBody.Model model = body.getModel();
            return new AliyunSmsCheckResult(
                    body.getCode(),
                    body.getMessage(),
                    body.getSuccess(),
                    model == null ? null : model.getOutId(),
                    model == null ? null : model.getVerifyResult()
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "验证码校验被中断");
        } catch (ExecutionException ex) {
            throw wrapException("验证码校验", ex.getCause() == null ? ex : ex.getCause());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrapException("验证码校验", ex);
        }
    }

    @PreDestroy
    public void destroy() {
        AsyncClient localClient = client;
        if (localClient == null) {
            return;
        }
        try {
            localClient.close();
        } catch (Exception ex) {
            log.warn("close aliyun sms client failed: {}", ex.getMessage());
        }
    }

    private AsyncClient getClient() {
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (client == null) {
                try {
                    ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.create();
                    if (StringUtils.hasText(properties.getEndpoint())) {
                        overrideConfiguration.setEndpointOverride(properties.getEndpoint());
                    }
                    client = AsyncClient.builder()
                            .region(properties.getRegion())
                            .credentialsProvider(buildCredentialProvider(properties))
                            .overrideConfiguration(overrideConfiguration)
                            .build();
                } catch (BusinessException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw wrapException("初始化阿里云短信客户端", ex);
                }
            }
            return client;
        }
    }

    private ICredentialProvider buildCredentialProvider(SmsLoginProperties smsProperties) {
        if (StringUtils.hasText(smsProperties.getAccessKeyId()) && StringUtils.hasText(smsProperties.getAccessKeySecret())) {
            Credential credential = Credential.builder()
                    .accessKeyId(smsProperties.getAccessKeyId())
                    .accessKeySecret(smsProperties.getAccessKeySecret())
                    .build();
            return StaticCredentialProvider.create(credential);
        }
        return EnvironmentVariableCredentialProvider.create();
    }

    private BusinessException wrapException(String action, Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        if (message == null) {
            message = "unknown error";
        }
        if (message.contains("accessKeyId/accessKeySecret cannot be empty")) {
            return new BusinessException(500,
                    action + "失败：阿里云 AK/SK 未配置。请设置环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET，或在 application.yml 的 sms.access-key-id / sms.access-key-secret 配置。");
        }
        if (message.contains("credentials file is not exist")) {
            return new BusinessException(500,
                    action + "失败：当前环境配置了阿里云 Profile 但未找到凭证文件。建议改为配置 AK/SK（环境变量或 application.yml）。");
        }
        return new BusinessException(502, action + "失败：" + message);
    }
}

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
                throw new BusinessException(502, "第三方服务调用失败");
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
            throw new BusinessException(500, "短信服务请求被中断");
        } catch (ExecutionException ex) {
            throw wrapException("发送短信验证码失败", ex.getCause() == null ? ex : ex.getCause());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrapException("发送短信验证码失败", ex);
        }
    }

    public AliyunSmsCheckResult checkSmsVerifyCode(CheckSmsVerifyCodeRequest request) {
        try {
            CheckSmsVerifyCodeResponse response = getClient().checkSmsVerifyCode(request).get();
            CheckSmsVerifyCodeResponseBody body = response == null ? null : response.getBody();
            if (body == null) {
                throw new BusinessException(502, "第三方服务调用失败");
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
            throw new BusinessException(500, "短信服务请求被中断");
        } catch (ExecutionException ex) {
            throw wrapException("校验短信验证码失败", ex.getCause() == null ? ex : ex.getCause());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrapException("校验短信验证码失败", ex);
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
                    throw wrapException("初始化短信客户端失败", ex);
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
            message = "未知错误";
        }
        if (message.contains("accessKeyId/accessKeySecret cannot be empty")) {
            return new BusinessException(500,
                    action + "：未配置阿里云短信 AK/SK，请在环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID / ALIBABA_CLOUD_ACCESS_KEY_SECRET 或 application.yml 的 sms.access-key-id / sms.access-key-secret 中配置。");
        }
        if (message.contains("credentials file is not exist")) {
            return new BusinessException(500,
                    action + "：未找到可用的阿里云凭证，请检查运行环境凭证或 application.yml 配置。");
        }
        return new BusinessException(502, action + "，第三方短信服务调用失败");
    }
}

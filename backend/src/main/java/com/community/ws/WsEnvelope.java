package com.community.ws;

import lombok.Data;

@Data
public class WsEnvelope {

    private String type;
    private String requestId;
    private Integer code;
    private String message;
    private Object payload;
    private Long ts;

    public static WsEnvelope event(String type, Object payload) {
        WsEnvelope envelope = new WsEnvelope();
        envelope.setType(type);
        envelope.setCode(0);
        envelope.setMessage("ok");
        envelope.setPayload(payload);
        envelope.setTs(System.currentTimeMillis());
        return envelope;
    }

    public static WsEnvelope response(String type, String requestId, Object payload) {
        WsEnvelope envelope = event(type, payload);
        envelope.setRequestId(requestId);
        return envelope;
    }

    public static WsEnvelope error(String requestId, int code, String message) {
        WsEnvelope envelope = new WsEnvelope();
        envelope.setType("ERROR");
        envelope.setRequestId(requestId);
        envelope.setCode(code);
        envelope.setMessage(message);
        envelope.setTs(System.currentTimeMillis());
        return envelope;
    }
}


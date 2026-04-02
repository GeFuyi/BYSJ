package com.community.ws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WsOfflineQueueService {

    private static final Logger log = LoggerFactory.getLogger(WsOfflineQueueService.class);
    private static final String OFFLINE_QUEUE_PREFIX = "ws:offline:";
    private static final long OFFLINE_MAX = 500;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public WsOfflineQueueService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(Long userId, WsEnvelope envelope) {
        String key = key(userId);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            redisTemplate.opsForList().rightPush(key, json);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > OFFLINE_MAX) {
                redisTemplate.opsForList().trim(key, size - OFFLINE_MAX, -1);
            }
        } catch (Exception ex) {
            log.warn("enqueue offline ws message failed, userId={}, err={}", userId, ex.getMessage());
        }
    }

    public List<WsEnvelope> pullAll(Long userId) {
        String key = key(userId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        redisTemplate.delete(key);
        List<WsEnvelope> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return list;
        }
        for (Object item : raw) {
            if (item == null) {
                continue;
            }
            try {
                WsEnvelope envelope = objectMapper.readValue(String.valueOf(item), new TypeReference<WsEnvelope>() {
                });
                list.add(envelope);
            } catch (Exception ex) {
                log.warn("parse offline ws message failed: {}", ex.getMessage());
            }
        }
        return list;
    }

    private String key(Long userId) {
        return OFFLINE_QUEUE_PREFIX + userId;
    }
}


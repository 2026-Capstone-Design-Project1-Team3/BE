package com.server.talkup_be.repo;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class EmitterRepo {
    // 안전성을 위해 ConcurrentHashMap 사용
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(String fileKey, SseEmitter emitter) {
        emitters.put(fileKey, emitter);
    }

    public SseEmitter get(String fileKey) {
        return emitters.get(fileKey);
    }

    public void delete(String fileKey) {
        emitters.remove(fileKey);
    }
}
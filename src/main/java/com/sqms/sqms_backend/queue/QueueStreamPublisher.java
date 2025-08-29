package com.sqms.sqms_backend.queue;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueueStreamPublisher {
    private final Map<Long, List<SseEmitter>> emittersByDoctor = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long doctorId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emittersByDoctor.computeIfAbsent(doctorId, k -> new ArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(doctorId, emitter));
        emitter.onTimeout(() -> remove(doctorId, emitter));
        return emitter;
    }

    public void pushDoctorUpdate(Long doctorId) {
        var list = emittersByDoctor.getOrDefault(doctorId, List.of());
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name("queue-update").data("updated"));
            } catch (IOException ex) {
                dead.add(e);
            }
        }
        list.removeAll(dead);
    }

    private void remove(Long doctorId, SseEmitter e) {
        emittersByDoctor.computeIfPresent(doctorId, (k, v) -> { v.remove(e); return v; });
    }
}
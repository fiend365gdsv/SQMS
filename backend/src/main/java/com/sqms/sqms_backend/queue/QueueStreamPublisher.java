package com.sqms.sqms_backend.queue;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueueStreamPublisher {

    // Map of doctorId -> list of active emitters
    private final Map<Long, List<SseEmitter>> emittersByDoctor = new ConcurrentHashMap<>();

    /**
     * Subscribe to SSE updates for a doctor.
     * @param doctorId doctor's ID
     * @return SseEmitter to receive events
     */
    public SseEmitter subscribe(Long doctorId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        // Add emitter in a thread-safe way
        emittersByDoctor.compute(doctorId, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(emitter);
            return v;
        });

        // Remove emitter on completion or timeout
        emitter.onCompletion(() -> removeEmitter(doctorId, emitter));
        emitter.onTimeout(() -> removeEmitter(doctorId, emitter));

        return emitter;
    }

    /**
     * Push an update to all active emitters for a doctor.
     * @param doctorId doctor's ID
     */
    public void pushDoctorUpdate(Long doctorId) {
        List<SseEmitter> emitters = emittersByDoctor.getOrDefault(doctorId, new ArrayList<>());

        // Make a mutable copy for safe iteration
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter e : new ArrayList<>(emitters)) {
            try {
                e.send(SseEmitter.event().name("queue-update").data("updated"));
            } catch (IOException ex) {
                deadEmitters.add(e);
            }
        }

        // Remove dead emitters safely
        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
    }

    /**
     * Remove a single emitter from a doctor's list.
     */
    private void removeEmitter(Long doctorId, SseEmitter emitter) {
        emittersByDoctor.computeIfPresent(doctorId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list; // remove key if list empty
        });
    }
}

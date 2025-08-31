package com.sqms.sqms_backend.queue;

import com.sqms.sqms_backend.patient.Patient;
import com.sqms.sqms_backend.patient.PatientRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/queue")
@CrossOrigin(origins = "*")
public class QueueController {

    private final QueueService queueService;
    private final QueueStreamPublisher stream;
    private final PatientRepository patientRepo;

    public QueueController(QueueService queueService,
                           QueueStreamPublisher stream,
                           PatientRepository patientRepo) {
        this.queueService = queueService;
        this.stream = stream;
        this.patientRepo = patientRepo;
    }

    // Reception: create patient
    @PostMapping("/patients")
    public Patient createPatient(@RequestBody Patient p) {
        return patientRepo.save(p);
    }

    // Reception: enqueue patient
    @PostMapping("/{doctorId}/enqueue")
    public Token enqueue(@PathVariable Long doctorId, @RequestParam Long patientId) {
        return queueService.enqueue(doctorId, patientId);
    }

    // Doctor: call next patient
    @PostMapping("/{doctorId}/call-next")
    public Token callNext(@PathVariable Long doctorId) {
        return queueService.callNext(doctorId);
    }

    // Doctor: mark served
    @PostMapping("/tokens/{tokenId}/served")
    public Token served(@PathVariable Long tokenId) {
        return queueService.markServed(tokenId);
    }

    // Mark absent & requeue
    @PostMapping("/tokens/{tokenId}/absent")
    public Token absent(@PathVariable Long tokenId) {
        return queueService.markAbsentAndRequeue(tokenId);
    }

    // Waiting list with ETA
    @GetMapping("/{doctorId}/waiting")
    public List<Map<String, Object>> waiting(@PathVariable Long doctorId) {
        List<Token> list = queueService.waitingList(doctorId);
        int avg = queueService.averageServiceSecondsToday(doctorId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Token t = list.get(i);
            out.add(Map.of(
                    "tokenId", t.getId(),
                    "tokenNumber", t.getTokenNumber(),
                    "patientName", t.getPatient().getName(),
                    "position", i + 1,
                    "etaSeconds", i * avg
            ));
        }
        return out;
    }

    // Pending tokens
    @GetMapping("/{doctorId}/pending")
    public List<Token> pending(@PathVariable Long doctorId) {
        return queueService.pendingTokens(doctorId);
    }

    // Completed tokens
    @GetMapping("/{doctorId}/completed")
    public List<Token> completed(@PathVariable Long doctorId) {
        return queueService.completedTokens(doctorId);
    }

    // SSE subscription
    @GetMapping(value = "/stream/{doctorId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long doctorId) {
        return stream.subscribe(doctorId);
    }
}

package com.sqms.sqms_backend.queue;

import com.sqms.sqms_backend.doctor.Doctor;
import com.sqms.sqms_backend.doctor.DoctorRepository;
import com.sqms.sqms_backend.patient.Patient;
import com.sqms.sqms_backend.patient.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class QueueService {

    private final TokenRepository tokenRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;
    private final QueueStreamPublisher stream;

    public QueueService(TokenRepository tokenRepo, DoctorRepository doctorRepo,
                        PatientRepository patientRepo, QueueStreamPublisher stream) {
        this.tokenRepo = tokenRepo;
        this.doctorRepo = doctorRepo;
        this.patientRepo = patientRepo;
        this.stream = stream;
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    @Transactional
    public Token enqueue(Long doctorId, Long patientId) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow();
        Patient patient = patientRepo.findById(patientId).orElseThrow();

        Long nextOrder = tokenRepo.findMaxQueueOrder(doctor) + 1;
        Integer nextTokenNum = tokenRepo.findMaxTokenNumberForDay(doctor, startOfToday()) + 1;

        Token token = new Token();
        token.setDoctor(doctor);
        token.setPatient(patient);
        token.setQueueOrder(nextOrder);
        token.setTokenNumber(nextTokenNum);
        token.setStatus(TokenStatus.WAITING);
        token.setMissedCount(0);
        token.setServiceDay(startOfToday());

        Token saved = tokenRepo.save(token);
        stream.pushDoctorUpdate(doctorId);
        return saved;
    }

    @Transactional
    public Token callNext(Long doctorId) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow();
        Token next = tokenRepo.findFirstByDoctorAndStatusOrderByQueueOrderAsc(doctor, TokenStatus.WAITING)
                .orElseThrow(() -> new IllegalStateException("No waiting patients"));
        next.setStatus(TokenStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());
        Token saved = tokenRepo.save(next);
        stream.pushDoctorUpdate(doctorId);
        return saved;
    }

    @Transactional
    public Token markServed(Long tokenId) {
        Token t = tokenRepo.findById(tokenId).orElseThrow();
        t.setStatus(TokenStatus.SERVED);
        t.setServedAt(LocalDateTime.now());
        if (t.getCalledAt() != null) {
            t.setServiceSeconds((int) Duration.between(t.getCalledAt(), t.getServedAt()).getSeconds());
        }
        Token saved = tokenRepo.save(t);
        stream.pushDoctorUpdate(t.getDoctor().getId());
        return saved;
    }

    @Transactional
    public Token markAbsentAndRequeue(Long tokenId) {
        Token t = tokenRepo.findById(tokenId).orElseThrow();
        Long doctorId = t.getDoctor().getId();
        Long nextOrder = tokenRepo.findMaxQueueOrder(t.getDoctor()) + 1;

        t.setStatus(TokenStatus.WAITING);
        t.setQueueOrder(nextOrder);
        t.setMissedCount((t.getMissedCount() == null ? 0 : t.getMissedCount()) + 1);
        t.setCalledAt(null);

        Token saved = tokenRepo.save(t);
        stream.pushDoctorUpdate(doctorId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Token> waitingList(Long doctorId) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow();
        return tokenRepo.findWaitingByDoctor(doctor);
    }

    @Transactional(readOnly = true)
    public int averageServiceSecondsToday(Long doctorId) {
        Doctor doctor = doctorRepo.findById(doctorId).orElseThrow();
        List<Token> served = tokenRepo.findServedToday(doctor, startOfToday());
        return (int) served.stream()
                .map(Token::getServiceSeconds)
                .filter(x -> x != null && x > 0)
                .mapToInt(Integer::intValue)
                .limit(30)
                .average()
                .orElse(180);
    }
}

package com.sqms.sqms_backend.queue;

import com.sqms.sqms_backend.doctor.Doctor;
import com.sqms.sqms_backend.patient.Patient;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name="idx_token_doctor_status_order", columnList="doctor_id,status,queueOrder"),
})
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private Doctor doctor;

    @ManyToOne(optional=false)
    private Patient patient;

    private Integer tokenNumber;
    private Long queueOrder;

    @Enumerated(EnumType.STRING)
    private TokenStatus status;

    private Integer missedCount;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime servedAt;
    private Integer serviceSeconds;
    private LocalDateTime serviceDay;

    // Default constructor
    public Token() {}

    // Constructor with all fields (optional)
    public Token(Long id, Doctor doctor, Patient patient, Integer tokenNumber, Long queueOrder,
                 TokenStatus status, Integer missedCount, LocalDateTime createdAt,
                 LocalDateTime calledAt, LocalDateTime servedAt, Integer serviceSeconds,
                 LocalDateTime serviceDay) {
        this.id = id;
        this.doctor = doctor;
        this.patient = patient;
        this.tokenNumber = tokenNumber;
        this.queueOrder = queueOrder;
        this.status = status;
        this.missedCount = missedCount;
        this.createdAt = createdAt;
        this.calledAt = calledAt;
        this.servedAt = servedAt;
        this.serviceSeconds = serviceSeconds;
        this.serviceDay = serviceDay;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }

    public Long getQueueOrder() { return queueOrder; }
    public void setQueueOrder(Long queueOrder) { this.queueOrder = queueOrder; }

    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }

    public Integer getMissedCount() { return missedCount; }
    public void setMissedCount(Integer missedCount) { this.missedCount = missedCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }

    public LocalDateTime getServedAt() { return servedAt; }
    public void setServedAt(LocalDateTime servedAt) { this.servedAt = servedAt; }

    public Integer getServiceSeconds() { return serviceSeconds; }
    public void setServiceSeconds(Integer serviceSeconds) { this.serviceSeconds = serviceSeconds; }

    public LocalDateTime getServiceDay() { return serviceDay; }
    public void setServiceDay(LocalDateTime serviceDay) { this.serviceDay = serviceDay; }
}

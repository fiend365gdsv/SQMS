package com.sqms.sqms_backend.queue;

import com.sqms.sqms_backend.doctor.Doctor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("SELECT COALESCE(MAX(t.queueOrder),0) FROM Token t WHERE t.doctor = :doctor")
    Long findMaxQueueOrder(@Param("doctor") Doctor doctor);

    @Query("SELECT COALESCE(MAX(t.tokenNumber),0) FROM Token t WHERE t.doctor = :doctor AND t.serviceDay = :day")
    Integer findMaxTokenNumberForDay(@Param("doctor") Doctor doctor, @Param("day") LocalDateTime dayStart);

    List<Token> findTop10ByDoctorAndStatusOrderByQueueOrderAsc(Doctor doctor, TokenStatus status);

    Optional<Token> findFirstByDoctorAndStatusOrderByQueueOrderAsc(Doctor doctor, TokenStatus status);

    @Query("SELECT t FROM Token t WHERE t.doctor = :doctor AND t.status = 'WAITING' ORDER BY t.queueOrder ASC")
    List<Token> findWaitingByDoctor(@Param("doctor") Doctor doctor);

    @Query("SELECT t FROM Token t WHERE t.doctor = :doctor AND t.status = 'SERVED' AND t.serviceDay = :day ORDER BY t.servedAt DESC")
    List<Token> findServedToday(@Param("doctor") Doctor doctor, @Param("day") LocalDateTime dayStart);
}
package com.aatechsolutions.elgransazon.domain.repository;

import com.aatechsolutions.elgransazon.domain.entity.DailyOrderCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyOrderCounterRepository extends JpaRepository<DailyOrderCounter, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DailyOrderCounter> findByDate(LocalDate date);
}

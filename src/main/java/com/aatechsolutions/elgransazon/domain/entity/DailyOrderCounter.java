package com.aatechsolutions.elgransazon.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_order_counters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyOrderCounter {

    @Id
    @Column(name = "counter_date")
    private LocalDate date;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;
}

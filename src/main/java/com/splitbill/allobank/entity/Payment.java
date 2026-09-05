package com.splitbill.allobank.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_participant_id")
    private Participant from; // siapa yang bayar

    @ManyToOne
    @JoinColumn(name = "to_participant_id")
    private Participant to; // siapa yang menerima

    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "group_id")   // 🔑 tambahkan relasi ke Group
    private Group group;

    private LocalDateTime paymentDate = LocalDateTime.now();
}

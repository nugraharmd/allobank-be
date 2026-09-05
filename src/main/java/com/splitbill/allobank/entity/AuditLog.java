package com.splitbill.allobank.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;
    private Long entityId;
    private String action;
    private BigDecimal amount;
    private String performedBy;
    private Long groupId;
    private LocalDateTime timestamp;

    @Lob
    private String beforeState;
    @Lob
    private String afterState;

}

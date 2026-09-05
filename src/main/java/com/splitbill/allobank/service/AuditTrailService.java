package com.splitbill.allobank.service;

import com.splitbill.allobank.entity.AuditLog;
import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Payment;
import com.splitbill.allobank.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditTrailService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void recordExpenseAudit(Expense expense, String action, String performedBy,
                                   String beforeState, String afterState) {
        AuditLog log = new AuditLog();
        log.setEntityType("EXPENSE");
        log.setEntityId(expense.getId());
        log.setAction(action);
        log.setAmount(expense.getAmount());
        log.setPerformedBy(performedBy);
        log.setGroupId(expense.getGroup().getId());
        log.setTimestamp(LocalDateTime.now());
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        auditLogRepository.save(log);
    }

    public void recordPaymentAudit(Payment payment, String action, String performedBy,
                                   String beforeState, String afterState) {
        AuditLog log = new AuditLog();
        log.setEntityType("PAYMENT");
        log.setEntityId(payment.getId());
        log.setAction(action);
        log.setAmount(payment.getAmount());
        log.setPerformedBy(performedBy);
        log.setGroupId(payment.getGroup().getId());
        log.setTimestamp(LocalDateTime.now());
        log.setBeforeState(beforeState);
        log.setAfterState(afterState);
        auditLogRepository.save(log);
    }
}

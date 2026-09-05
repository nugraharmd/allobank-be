package com.splitbill.allobank.service;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import com.splitbill.allobank.entity.Participant;
import com.splitbill.allobank.entity.Payment;
import com.splitbill.allobank.enums.SplitStrategy;
import com.splitbill.allobank.repository.ExpenseRepository;
import com.splitbill.allobank.repository.PaymentRepository;
import com.splitbill.allobank.response.SettlementResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class SettlementService {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    private AuditTrailService auditTrailService;

    public int calculateServiceChargePct(String githubUsername) {
        int sum = 0;
        for (char c : githubUsername.toLowerCase().toCharArray()) {
            sum += (int) c;
        }
        return sum % 10;
    }

    public BigDecimal calculateServiceChargeAmount(BigDecimal totalExpenses, int pct) {
        return totalExpenses.multiply(BigDecimal.valueOf(pct))
                            .divide(BigDecimal.valueOf(100));
    }


    public SettlementResponse calculateSettlement(Group group,
                                                  List<Expense> expenses,
                                                  String githubUsername) {
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map untuk menyimpan total "owed" per participant
        Map<Participant, BigDecimal> owedMap = new HashMap<>();

        // Hitung owed sesuai strategi tiap expense
        for (Expense expense : expenses) {
            if (expense.getSplitStrategy() == null) {
                expense.setSplitStrategy(SplitStrategy.EQUAL);
            }
            switch (expense.getSplitStrategy()) {
                case EQUAL:
                    BigDecimal equalShare = expense.getAmount()
                            .divide(BigDecimal.valueOf(expense.getBeneficiaries().size()), 2, RoundingMode.HALF_UP);
                    for (Participant beneficiary : expense.getBeneficiaries()) {
                        owedMap.merge(beneficiary, equalShare, BigDecimal::add);
                    }
                    break;

                case PERCENTAGE:
                    for (Participant beneficiary : expense.getBeneficiaries()) {
                        BigDecimal percentage = expense.getPercentageShares().get(beneficiary.getId());
                        if (percentage != null) {
                            BigDecimal owed = expense.getAmount()
                                    .multiply(percentage)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            owedMap.merge(beneficiary, owed, BigDecimal::add);
                        }
                    }
                    break;

                case EXACT:
                    for (Participant beneficiary : expense.getBeneficiaries()) {
                        BigDecimal owed = expense.getExactShares().get(beneficiary.getId());
                        if (owed != null) {
                            owedMap.merge(beneficiary, owed, BigDecimal::add);
                        }
                    }
                    break;
            }
        }

        // Hitung balance tiap participant dari expenses
        Map<Participant, Map<String, Object>> balancesMap = new HashMap<>();
        for (Participant p : group.getParticipants()) {
            BigDecimal paid = expenses.stream()
                    .filter(e -> e.getPayer().equals(p))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal owed = owedMap.getOrDefault(p, BigDecimal.ZERO);
            BigDecimal balance = paid.subtract(owed);

            Map<String, Object> entry = new HashMap<>();
            entry.put("participant", p.getName());
            entry.put("paid", paid);
            entry.put("owed", owed);
            entry.put("balance", balance);

            balancesMap.put(p, entry);
        }

        // 🔑 Integrasi Payment
        List<Payment> payments = paymentRepository.findByGroup(group);
        for (Payment payment : payments) {
            Participant from = payment.getFrom();
            Participant to = payment.getTo();
            BigDecimal amount = payment.getAmount();

            // Hutang berkurang untuk payer
            Map<String, Object> fromEntry = balancesMap.get(from);
            if (fromEntry != null) {
                BigDecimal fromBalance = (BigDecimal) fromEntry.get("balance");
                fromEntry.put("balance", fromBalance.add(amount));
            }

            // Piutang berkurang untuk receiver
            Map<String, Object> toEntry = balancesMap.get(to);
            if (toEntry != null) {
                BigDecimal toBalance = (BigDecimal) toEntry.get("balance");
                toEntry.put("balance", toBalance.subtract(amount));
            }
        }

        // Konversi ke list untuk response
        List<Map<String, Object>> balances = new ArrayList<>(balancesMap.values());

        // Hitung service charge
        int serviceChargePct = calculateServiceChargePct(githubUsername);
        BigDecimal serviceChargeAmount = calculateServiceChargeAmount(totalExpenses, serviceChargePct);

        SettlementResponse response = new SettlementResponse();
        response.setTotalExpenses(totalExpenses);
        response.setBalances(balances);
        response.setServiceChargePct(serviceChargePct);
        response.setServiceChargeAmount(serviceChargeAmount);

        return response;
    }



    public void recordPayment(Group group, Participant from, Participant to, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setGroup(group);
        payment.setFrom(from);
        payment.setTo(to);
        payment.setAmount(amount);
        paymentRepository.save(payment);

        // Update balances
        SettlementResponse settlement = calculateSettlement(group, expenseRepository.findByGroup(group), "githubUser");
        BigDecimal oldBalance = null;
        for (Map<String, Object> balance : settlement.getBalances()) {
            if (balance.get("participant").equals(from.getName())) {
                 oldBalance = (BigDecimal) balance.get("balance");
                balance.put("balance", oldBalance.add(amount)); // hutang berkurang
            }
            if (balance.get("participant").equals(to.getName())) {
                 oldBalance = (BigDecimal) balance.get("balance");
                balance.put("balance", oldBalance.subtract(amount)); // piutang berkurang
            }
        }

        auditTrailService.recordPaymentAudit(payment,"CREATE_PAYMENT", from.getName(), oldBalance.toString(),amount.toString());
    }


}

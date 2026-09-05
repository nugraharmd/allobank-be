package com.splitbill.allobank.service;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import com.splitbill.allobank.entity.Participant;
import com.splitbill.allobank.entity.Payment;
import com.splitbill.allobank.enums.SplitStrategy;
import com.splitbill.allobank.repository.ExpenseRepository;
import com.splitbill.allobank.repository.PaymentRepository;
import com.splitbill.allobank.response.SettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SettlementServiceTest {

    @Autowired
    private SettlementService settlementService;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    @MockitoBean
    private AuditTrailService auditTrailService;

    private Group group;
    private Participant alice, bob;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(1L);

        alice = new Participant();
        alice.setId(1L);
        alice.setName("Alice");

        bob = new Participant();
        bob.setId(2L);
        bob.setName("Bob");

        group.setParticipants(Arrays.asList(alice, bob));
    }

    @Test
    void testCalculateServiceChargePct() {
        int pct = settlementService.calculateServiceChargePct("Alice");
        assertTrue(pct >= 0 && pct < 10);
    }

    @Test
    void testCalculateServiceChargeAmount() {
        BigDecimal result = settlementService.calculateServiceChargeAmount(BigDecimal.valueOf(200), 10);
        assertEquals(BigDecimal.valueOf(20), result);
    }

    @Test
    void testEqualSplitSettlement() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setPayer(alice);
        expense.setBeneficiaries(Arrays.asList(alice, bob));
        expense.setSplitStrategy(SplitStrategy.EQUAL);

        when(paymentRepository.findByGroup(group)).thenReturn(Collections.emptyList());

        SettlementResponse response = settlementService.calculateSettlement(group, Collections.singletonList(expense), "Alice");

        assertEquals(BigDecimal.valueOf(100), response.getTotalExpenses());
        assertEquals(2, response.getBalances().size());
    }

    @Test
    void testPercentageSplitSettlement() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(100));
        expense.setPayer(alice);
        expense.setBeneficiaries(Arrays.asList(alice, bob));
        expense.setSplitStrategy(SplitStrategy.PERCENTAGE);

        Map<Long, BigDecimal> shares = new HashMap<>();
        shares.put(alice.getId(), BigDecimal.valueOf(70));
        shares.put(bob.getId(), BigDecimal.valueOf(30));
        expense.setPercentageShares(shares);

        when(paymentRepository.findByGroup(group)).thenReturn(Collections.emptyList());

        SettlementResponse response = settlementService.calculateSettlement(group, Collections.singletonList(expense), "Alice");

        assertEquals(BigDecimal.valueOf(100), response.getTotalExpenses());
    }

    @Test
    void testExactSplitSettlement() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(120));
        expense.setPayer(alice);
        expense.setBeneficiaries(Arrays.asList(alice, bob));
        expense.setSplitStrategy(SplitStrategy.EXACT);

        Map<Long, BigDecimal> shares = new HashMap<>();
        shares.put(alice.getId(), BigDecimal.valueOf(70));
        shares.put(bob.getId(), BigDecimal.valueOf(50));
        expense.setExactShares(shares);

        when(paymentRepository.findByGroup(group)).thenReturn(Collections.emptyList());

        SettlementResponse response = settlementService.calculateSettlement(group, Collections.singletonList(expense), "Alice");

        assertEquals(BigDecimal.valueOf(120), response.getTotalExpenses());
    }

    @Test
    void testSettlementWithPayment() {
        Expense expense = new Expense();
        expense.setAmount(BigDecimal.valueOf(150));
        expense.setPayer(alice);
        expense.setBeneficiaries(Arrays.asList(alice, bob));
        expense.setSplitStrategy(SplitStrategy.EQUAL);

        Payment payment = new Payment();
        payment.setGroup(group);
        payment.setFrom(bob);
        payment.setTo(alice);
        payment.setAmount(BigDecimal.valueOf(50));

        when(paymentRepository.findByGroup(group)).thenReturn(Collections.singletonList(payment));

        SettlementResponse response = settlementService.calculateSettlement(group, Collections.singletonList(expense), "Alice");

        assertEquals(BigDecimal.valueOf(150), response.getTotalExpenses());
    }

    @Test
    void testRecordPayment() {
        when(expenseRepository.findByGroup(group)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByGroup(group)).thenReturn(Collections.emptyList());

        settlementService.recordPayment(group, bob, alice, BigDecimal.valueOf(50));

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(auditTrailService, times(1))
                .recordPaymentAudit(any(Payment.class), eq("CREATE_PAYMENT"), eq("Bob"), anyString(), anyString());
    }
}

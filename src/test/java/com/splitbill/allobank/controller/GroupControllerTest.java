package com.splitbill.allobank.controller;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import com.splitbill.allobank.entity.Participant;
import com.splitbill.allobank.repository.ExpenseRepository;
import com.splitbill.allobank.repository.GroupRepository;
import com.splitbill.allobank.repository.ParticipantRepository;
import com.splitbill.allobank.request.ExpenseRequest;
import com.splitbill.allobank.request.PaymentRequest;
import com.splitbill.allobank.response.SettlementResponse;
import com.splitbill.allobank.service.AuditTrailService;
import com.splitbill.allobank.service.SettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupRepository groupRepository;

    @MockitoBean
    private ExpenseRepository expenseRepository;

    @MockitoBean
    private ParticipantRepository participantRepository;

    @MockitoBean
    private SettlementService settlementService;

    @MockitoBean
    private AuditTrailService auditTrailService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateGroup() throws Exception {
        Group group = new Group();
        group.setId(1L);
        group.setName("Trip Bali");

        when(groupRepository.save(any(Group.class))).thenReturn(group);

        mockMvc.perform(post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trip Bali"));
    }

    @Test
    void testAddExpense() throws Exception {
        Group group = new Group();
        group.setId(1L);

        Participant alice = new Participant();
        alice.setId(1L);
        alice.setName("Alice");

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setGroup(group);
        expense.setPayer(alice);
        expense.setAmount(BigDecimal.valueOf(100));

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(participantRepository.findAllById(anyList())).thenReturn(List.of(alice));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);

        ExpenseRequest request = new ExpenseRequest();
        request.setPayerId(1L);
        request.setBeneficiaryIds(List.of(1L));
        request.setAmount(BigDecimal.valueOf(100));
        request.setCategory("Food");

        mockMvc.perform(post("/groups/1/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100));
    }

    @Test
    void testGetSettlement() throws Exception {
        Group group = new Group();
        group.setId(1L);

        SettlementResponse response = new SettlementResponse();
        response.setTotalExpenses(BigDecimal.valueOf(150));
        response.setServiceChargePct(2);
        response.setServiceChargeAmount(BigDecimal.valueOf(3));
        response.setBalances(Collections.emptyList());

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(expenseRepository.findAll()).thenReturn(Collections.emptyList());
        when(settlementService.calculateSettlement(eq(group), anyList(), eq("nugraharmd")))
                .thenReturn(response);

        mockMvc.perform(get("/groups/1/settlement")
                .param("githubUsername", "nugraharmd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(150))
                .andExpect(jsonPath("$.serviceChargePct").value(2))
                .andExpect(jsonPath("$.serviceChargeAmount").value(3));
    }

    @Test
    void testRecordPayment() throws Exception {
        Group group = new Group();
        group.setId(1L);

        Participant from = new Participant();
        from.setId(1L);
        from.setName("Alice");

        Participant to = new Participant();
        to.setId(2L);
        to.setName("Bob");

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(participantRepository.findById(1L)).thenReturn(Optional.of(from));
        when(participantRepository.findById(2L)).thenReturn(Optional.of(to));

        PaymentRequest request = new PaymentRequest();
        request.setFromId(1L);
        request.setToId(2L);
        request.setAmount(BigDecimal.valueOf(50));

        mockMvc.perform(post("/groups/1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(settlementService, times(1))
                .recordPayment(eq(group), eq(from), eq(to), eq(BigDecimal.valueOf(50)));
    }
}

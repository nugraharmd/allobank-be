package com.splitbill.allobank.controller;

import com.splitbill.allobank.entity.Expense;
import com.splitbill.allobank.entity.Group;
import com.splitbill.allobank.entity.Participant;
import com.splitbill.allobank.enums.SplitStrategy;
import com.splitbill.allobank.repository.ExpenseRepository;
import com.splitbill.allobank.repository.GroupRepository;
import com.splitbill.allobank.repository.ParticipantRepository;
import com.splitbill.allobank.request.ExpenseRequest;
import com.splitbill.allobank.request.PaymentRequest;
import com.splitbill.allobank.response.SettlementResponse;
import com.splitbill.allobank.service.AuditTrailService;
import com.splitbill.allobank.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    public Group createGroup(@RequestBody Group group) {
        if (group.getParticipants() != null) {
            group.getParticipants().forEach(p -> p.setGroup(group));
        }
        return groupRepository.save(group);
    }


    @PostMapping("/{id}/expenses")
    public Expense addExpense(@PathVariable Long id, @RequestBody ExpenseRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Participant payer = participantRepository.findById(request.getPayerId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));

        List<Participant> beneficiaries = participantRepository.findAllById(request.getBeneficiaryIds());

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPayer(payer);
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setBeneficiaries(beneficiaries);

        // 🔑 Bagian ini WAJIB ada agar field tidak kosong
        expense.setSplitStrategy(request.getSplitStrategy() != null ? request.getSplitStrategy() : SplitStrategy.EQUAL);
        expense.setPercentageShares(request.getPercentageShares() != null ? request.getPercentageShares() : new HashMap<>());
        expense.setExactShares(request.getExactShares() != null ? request.getExactShares() : new HashMap<>());

        auditTrailService.recordExpenseAudit(expense,"CREATE_EXPENSE", payer.getName(), null, objectMapper.writeValueAsString(expense));
        return expenseRepository.save(expense);
    }



    @GetMapping("/{id}/settlement")
    public SettlementResponse getSettlement(@PathVariable Long id,
                                            @RequestParam String githubUsername) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        List<Expense> groupExpenses = expenseRepository.findAll()
                .stream()
                .filter(e -> e.getGroup().getId().equals(id))
                .toList();

        return settlementService.calculateSettlement(group, groupExpenses, githubUsername);
    }

    @PostMapping("/{groupId}/payments")
    public void recordPayment(@PathVariable Long groupId,
                              @RequestBody PaymentRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Participant from = participantRepository.findById(request.getFromId())
                .orElseThrow(() -> new RuntimeException("Payer not found"));
        Participant to = participantRepository.findById(request.getToId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        settlementService.recordPayment(group, from, to, request.getAmount());
    }

}

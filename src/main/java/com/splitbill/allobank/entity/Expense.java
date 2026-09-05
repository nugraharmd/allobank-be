package com.splitbill.allobank.entity;

import com.splitbill.allobank.enums.SplitStrategy;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Data
//@Entity
//@Table(name = "expenses")
//public class Expense {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//    private BigDecimal amount;
//    private String category;
//
//    @ManyToOne
//    @JoinColumn(name = "group_id")
//    private Group group;
//
//    @ManyToOne
//    @JoinColumn(name = "payer_id")
//    private Participant payer;
//
//    @ManyToMany
//    @JoinTable(
//        name = "expense_beneficiaries",
//        joinColumns = @JoinColumn(name = "expense_id"),
//        inverseJoinColumns = @JoinColumn(name = "participant_id")
//    )
//    private List<Participant> beneficiaries;
//
//}

@Data
@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private String category;

    @Enumerated(EnumType.STRING)
    private SplitStrategy splitStrategy = SplitStrategy.EQUAL; // default

    @ElementCollection
    @CollectionTable(name = "expense_percentage_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "participant_id")
    @Column(name = "percentage")
    private Map<Long, BigDecimal> percentageShares = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "expense_exact_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "participant_id")
    @Column(name = "amount")
    private Map<Long, BigDecimal> exactShares = new HashMap<>();

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "payer_id")
    private Participant payer;

    @ManyToMany
    @JoinTable(
            name = "expense_beneficiaries",
            joinColumns = @JoinColumn(name = "expense_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private List<Participant> beneficiaries = new ArrayList<>();
}

package com.splitbill.allobank.request;

import com.splitbill.allobank.enums.SplitStrategy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseRequest {
    private Long payerId;
    private BigDecimal amount;
    private List<Long> beneficiaryIds;
    private String category;
    private SplitStrategy splitStrategy;
    private Map<Long, BigDecimal> percentageShares; // beneficiaryId -> %
    private Map<Long, BigDecimal> exactShares;
}




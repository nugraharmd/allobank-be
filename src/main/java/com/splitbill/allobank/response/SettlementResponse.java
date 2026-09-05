package com.splitbill.allobank.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private BigDecimal totalExpenses;
    private BigDecimal sharePerPerson;
    private List<Map<String, Object>> balances;
    private int serviceChargePct;
    private BigDecimal serviceChargeAmount;
}
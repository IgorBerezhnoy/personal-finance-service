package com.personalfinance.persistence.dto;

import java.util.List;

public record WalletData(List<TransactionData> transactions, List<CategoryData> categories,
                         List<BudgetData> budgets) {
}

package com.personalfinance.persistence.dto;

import com.personalfinance.model.TransactionType;

public record TransactionData(String amount, TransactionType type, String category, String createdAt) {
}

package com.personalfinance.persistence.dto;

import com.personalfinance.model.TransactionType;

public record CategoryData(String name, TransactionType type) {
}

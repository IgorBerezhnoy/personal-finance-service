package com.personalfinance.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static java.math.BigDecimal.ZERO;

public class Transaction {
    private final BigDecimal amount;
    private final TransactionType type;
    private final String category;
    private final LocalDateTime createdAt;

    public Transaction(BigDecimal amount, TransactionType type, String category) {
        if ((amount == null)) {
            throw new IllegalArgumentException(
                    "Сумма не может быть null"
            );
        }

        if ((type == null)) {
            throw new IllegalArgumentException(
                    "Операция не может быть null"
            );
        }
        if ((category == null || category.isBlank())) {
            throw new IllegalArgumentException(
                    "Категория не может быть пустой"
            );
        }

        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумма должна быть больше нуля"
            );
        }

        this.amount = amount;
        this.type = type;
        this.category = category;

        this.createdAt = LocalDateTime.now();
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

package com.personalfinance.model;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

public class Budget {
    private final Category category;
    private BigDecimal limit;

    public Budget(Category category, BigDecimal limit) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "Категория не может быть null"
            );
        }
        if (category.getType() != TransactionType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Бюджет можно установить только для категории расходов"
            );
        }

        this.category = category;
        setLimit(limit);
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {

        if (limit == null) {
            throw new IllegalArgumentException(
                    "Лимит не может быть null"
            );
        }

        if (limit.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Лимит не может быть отрицательным"
            );
        }
        this.limit = limit;
    }
}

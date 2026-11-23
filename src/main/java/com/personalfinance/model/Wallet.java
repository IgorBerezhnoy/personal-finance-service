package com.personalfinance.model;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

public class Wallet {
    private BigDecimal balance;

    public Wallet() {
        balance = ZERO;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {

        if ((amount == null)) {
            throw new IllegalArgumentException(
                    "Сумма пополнения не может быть null"
            );
        }

        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумма пополнения должна быть больше нуля"
            );
        }

        balance = balance.add(amount);
    }
}

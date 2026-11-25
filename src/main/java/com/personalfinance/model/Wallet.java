package com.personalfinance.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.math.BigDecimal.ZERO;

public class Wallet {
    private BigDecimal balance;
    private final List<Transaction> transactions;

    public Wallet() {
        transactions = new ArrayList<>();
        balance = ZERO;

    }


    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    private void deposit(BigDecimal amount) {

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

    private void withdraw(BigDecimal amount) {

        if ((amount == null)) {
            throw new IllegalArgumentException(
                    "Сумма списания не может быть null"
            );
        }

        if (amount.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумма списания должна быть больше нуля"
            );
        }
        balance = balance.subtract(amount);

    }

    public void addTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Операция не может быть null"
            );
        }
        if (transaction.getType() == TransactionType.INCOME) {
            deposit(transaction.getAmount());

        } else if (transaction.getType() == TransactionType.EXPENSE) {
            withdraw(transaction.getAmount());
        } else {
            throw new IllegalArgumentException(
                    "Операция типа: " + transaction.getType() + " поддерживается"
            );
        }
        transactions.add(transaction);

    }

    public BigDecimal getTotalIncome() {
        BigDecimal sum = ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.INCOME) {
                sum = sum.add(transaction.getAmount());
            }
        }

        return sum;
    }

    public BigDecimal getTotalExpenses() {
        BigDecimal sum = ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.EXPENSE) {
                sum = sum.add(transaction.getAmount());
            }
        }

        return sum;
    }
}

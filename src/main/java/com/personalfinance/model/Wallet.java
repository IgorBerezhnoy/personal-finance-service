package com.personalfinance.model;

import java.math.BigDecimal;
import java.util.*;

import static java.math.BigDecimal.ZERO;

public class Wallet {
    private BigDecimal balance;
    private final List<Transaction> transactions;
    private final Map<String, Budget> budgets;


    public Wallet() {
        transactions = new ArrayList<>();
        balance = ZERO;
        budgets = new HashMap<>();
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
                    "Операция типа: " + transaction.getType() + " не поддерживается"
            );
        }
        transactions.add(transaction);

    }

    public void addBudget(Budget budget) {
        if (budget == null) {
            throw new IllegalArgumentException(
                    "Бюджет не может быть null"
            );
        }
        String categoryName = budget.getCategory().getName();

        if (budgets.containsKey(categoryName)) {
            throw new IllegalArgumentException("Бюджет для категории " + categoryName + " уже существует");
        }

        budgets.put(categoryName, budget);

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


    public Map<String, Budget> getBudgets() {
        return Collections.unmodifiableMap(budgets);
    }

    public BigDecimal getExpensesByCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException(
                    "Имя категории не может быть null"
            );
        }

        BigDecimal sum = ZERO;


        for (Transaction transaction : transactions) {
            if (transaction.getType() == TransactionType.EXPENSE && transaction.getCategory().equals(categoryName)) {

                sum = sum.add(transaction.getAmount());
            }
        }

        return sum;

    }

    public BigDecimal getRemainingBudget(String categoryName) {

        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException(
                    "Название категории не может быть пустым"
            );
        }

        Budget budgetsForCategory = budgets.get(categoryName);

        if (budgetsForCategory == null) {
            throw new IllegalArgumentException(
                    "Бюджет для категории '" + categoryName + "' не найден"
            );
        }

        BigDecimal categoryLimit = budgetsForCategory.getLimit();
        BigDecimal expensesForCategory = getExpensesByCategory(categoryName);

        return categoryLimit.subtract(expensesForCategory);

    }

    public boolean isBudgetExceeded(String categoryName) {
        return getRemainingBudget(categoryName).compareTo(ZERO) < 0;
    }
}

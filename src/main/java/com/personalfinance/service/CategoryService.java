
package com.personalfinance.service;

import com.personalfinance.model.Category;
import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.Wallet;

import java.util.List;
import java.util.Locale;

public class CategoryService {

    public void addCategory(Wallet wallet, Category category) {
        validateWallet(wallet);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Категория не может быть null"
            );
        }

        Category existing = findCategory(
                wallet,
                category.getName(),
                category.getType()
        );

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Категория '" + category.getName() + "' уже существует"
            );
        }

        wallet.addCategory(category);
    }

    public List<Category> getCategories(Wallet wallet) {
        validateWallet(wallet);
        return wallet.getCategories();
    }

    public Category findCategory(
            Wallet wallet,
            String name,
            TransactionType type
    ) {
        validateWallet(wallet);
        validateCategoryData(name, type);

        String normalizedName = normalizeName(name);

        for (Category category : wallet.getCategories()) {
            if (category.getType() == type
                    && normalizeName(category.getName()).equals(normalizedName)) {
                return category;
            }
        }

        return null;
    }

    public void removeCategory(
            Wallet wallet,
            String name,
            TransactionType type
    ) {
        validateWallet(wallet);
        validateCategoryData(name, type);

        Category category = findCategory(wallet, name, type);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Категория '" + name + "' не найдена"
            );
        }

        String normalizedName = normalizeName(name);

        for (Transaction transaction : wallet.getTransactions()) {
            if (transaction.getType() == type
                    && normalizeName(transaction.getCategory()).equals(normalizedName)) {
                throw new IllegalStateException(
                        "Нельзя удалить категорию с существующими операциями"
                );
            }
        }

        if (type == TransactionType.EXPENSE) {
            boolean hasBudget = wallet.getBudgets().values().stream()
                    .anyMatch(budget ->
                            normalizeName(budget.getCategory().getName())
                                    .equals(normalizedName)
                    );

            if (hasBudget) {
                throw new IllegalStateException(
                        "Нельзя удалить категорию с установленным бюджетом"
                );
            }
        }

        wallet.removeCategory(category);
    }

    private void validateWallet(Wallet wallet) {
        if (wallet == null) {
            throw new IllegalArgumentException(
                    "Кошелёк не может быть null"
            );
        }
    }

    private void validateCategoryData(String name, TransactionType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Название категории не может быть пустым"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Тип категории не может быть null"
            );
        }
    }

    private String normalizeName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}

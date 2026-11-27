
package com.personalfinance.service;

import com.personalfinance.model.Category;
import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.Wallet;

import java.util.List;

public class CategoryService {

    public void addCategory(Wallet wallet, Category category) {
        validateWallet(wallet);

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
        return wallet.findCategory(name, type);
    }

    public void removeCategory(
            Wallet wallet,
            String name,
            TransactionType type
    ) {
        validateWallet(wallet);
        Category category = findCategory(wallet, name, type);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Категория '" + name + "' не найдена"
            );
        }

        String normalizedName = Category.normalizeName(name);

        for (Transaction transaction : wallet.getTransactions()) {
            if (transaction.getType() == type
                    && Category.normalizeName(transaction.getCategory()).equals(normalizedName)) {
                throw new IllegalStateException(
                        "Нельзя удалить категорию с существующими операциями"
                );
            }
        }

        if (type == TransactionType.EXPENSE) {
            boolean hasBudget = wallet.getBudgets().values().stream()
                    .anyMatch(budget ->
                            Category.normalizeName(budget.getCategory().getName())
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

}

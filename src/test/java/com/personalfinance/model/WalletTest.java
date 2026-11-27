package com.personalfinance.model;

import com.personalfinance.service.CategoryService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletTest {
    @Test
    void walletAndServiceUseSameCategoryComparison() {
        String name = "  еДа\t";
        Wallet wallet = new Wallet();
        CategoryService service = new CategoryService();
        Category category = new Category(TransactionType.EXPENSE, "Еда");
        service.addCategory(wallet, category);

        assertSame(category, wallet.findCategory(name, TransactionType.EXPENSE));
        assertSame(category, service.findCategory(wallet, name, TransactionType.EXPENSE));
        assertThrows(IllegalArgumentException.class,
                () -> wallet.addCategory(new Category(TransactionType.EXPENSE, name)));
        assertThrows(IllegalArgumentException.class,
                () -> service.addCategory(wallet, new Category(TransactionType.EXPENSE, name)));
        wallet.addCategory(new Category(TransactionType.INCOME, name));
        assertEquals(2, wallet.getCategories().size());
    }

    @Test
    void budgetLookupAndCategoryExpensesUseSameComparison() {
        String name = "  еДа\t";
        Wallet wallet = new Wallet();
        Category category = new Category(TransactionType.EXPENSE, "Еда");
        wallet.addCategory(category);
        wallet.addBudget(new Budget(category, new BigDecimal("100.00")));
        wallet.addTransaction(new Transaction(new BigDecimal("25.50"), TransactionType.EXPENSE, name));
        wallet.addTransaction(new Transaction(new BigDecimal("5.25"), TransactionType.EXPENSE, " ЕДА "));
        wallet.addTransaction(new Transaction(new BigDecimal("500.00"), TransactionType.INCOME, "еда"));

        assertEquals(new BigDecimal("30.75"), wallet.getExpensesByCategory(name));
        assertEquals(new BigDecimal("69.25"), wallet.getRemainingBudget(name));
        assertEquals(new BigDecimal("469.25"), wallet.getBalance());
        assertFalse(wallet.isBudgetExceeded(name));
        assertThrows(IllegalArgumentException.class,
                () -> wallet.addBudget(new Budget(new Category(TransactionType.EXPENSE, name), BigDecimal.TEN)));
        assertEquals(1, wallet.getBudgets().size());
    }

}

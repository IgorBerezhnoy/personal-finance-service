package com.personalfinance.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.personalfinance.model.Budget;
import com.personalfinance.model.Category;
import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.User;
import com.personalfinance.model.Wallet;
import com.personalfinance.service.AuthService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileStorageTest {
    private static final String PASSWORD = "уникальный исходный пароль 🔐";
    private static final LocalDateTime ORIGINAL_DATE = LocalDateTime.of(2020, 2, 29, 13, 45, 12, 123456789);
    private static User credentials;
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path directory;

    @BeforeAll
    static void createCredentials() {
        credentials = new User("alice", PASSWORD);
    }

    @Test
    void savesAndRestoresWalletWithOriginalDatesAndPreciseAmounts() throws IOException {
        User original = sampleUser("alice");
        Path file = directory.resolve("nested/data/users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        storage.save(List.of(original));

        User restored = storage.load().getFirst();
        Wallet wallet = restored.getWallet();
        assertEquals(original.getLogin(), restored.getLogin());
        assertEquals(original.getPasswordHash(), restored.getPasswordHash());
        assertEquals(original.getPasswordSalt(), restored.getPasswordSalt());
        assertTrue(restored.checkPassword(PASSWORD));
        assertNotSame(original.getWallet(), wallet);
        assertEquals(2, wallet.getTransactions().size());
        for (int i = 0; i < 2; i++) {
            Transaction expected = original.getWallet().getTransactions().get(i);
            Transaction actual = wallet.getTransactions().get(i);
            assertEquals(expected.getAmount(), actual.getAmount());
            assertEquals(expected.getType(), actual.getType());
            assertEquals(expected.getCategory(), actual.getCategory());
            assertEquals(expected.getCreatedAt(), actual.getCreatedAt());
        }
        assertEquals(ORIGINAL_DATE, wallet.getTransactions().getFirst().getCreatedAt());
        assertEquals(original.getWallet().getBalance(), wallet.getBalance());
        assertEquals(original.getWallet().getTotalIncome(), wallet.getTotalIncome());
        assertEquals(original.getWallet().getTotalExpenses(), wallet.getTotalExpenses());
        assertEquals(original.getWallet().getRemainingBudget("Еда"), wallet.getRemainingBudget("Еда"));
        assertTrue(wallet.isBudgetExceeded("Еда"));
        assertEquals(List.of("Зарплата", "Еда"), wallet.getCategories().stream().map(Category::getName).toList());
        assertEquals(TransactionType.INCOME, wallet.getCategories().getFirst().getType());
        assertEquals(TransactionType.EXPENSE, wallet.getCategories().get(1).getType());
        assertEquals(new BigDecimal("20.00"), wallet.getBudgets().get("Еда").getLimit());
        assertSame(wallet.getCategories().get(1), wallet.getBudgets().get("Еда").getCategory());

        JsonNode json = mapper.readTree(file.toFile());
        assertTrue(json.at("/users/0/wallet/transactions/0/amount").isTextual());
        assertTrue(json.at("/users/0/wallet/transactions/0/createdAt").isTextual());
        assertTrue(json.at("/users/0/wallet/budgets/0/limit").isTextual());
        assertFalse(Files.readString(file).contains(PASSWORD));
        assertFalse(json.at("/users/0").has("password"));
        assertFalse(json.at("/users/0/wallet").has("balance"));
        assertNoTemporaryFiles(file.getParent());
    }

    @Test
    void rejectsOrphanBudgetAndRestoresWalletAfterCategoryIsRegistered() throws IOException {
        User original = emptyUser("alice");
        original.getWallet().addBudget(new Budget(new Category(TransactionType.EXPENSE, "Отдельная"), BigDecimal.ZERO));
        original.getWallet().addTransaction(new Transaction(new BigDecimal("10.00"),
                TransactionType.EXPENSE, "Без категории", ORIGINAL_DATE));
        JsonFileStorage storage = new JsonFileStorage(directory.resolve("users.json"));
        storage.save(List.of(original));

        IOException error = assertThrows(IOException.class, storage::load);
        assertTrue(error.getMessage().contains("Категория расходов 'Отдельная' для бюджета не найдена"));

        original.getWallet().addCategory(original.getWallet().getBudgets().get("Отдельная").getCategory());
        storage.save(List.of(original));
        Wallet restored = storage.load().getFirst().getWallet();
        assertEquals(1, restored.getCategories().size());
        assertEquals("Отдельная", restored.getCategories().getFirst().getName());
        assertEquals(new BigDecimal("-10.00"), restored.getBalance());
        assertEquals(BigDecimal.ZERO, restored.getBudgets().get("Отдельная").getLimit());
        assertEquals(TransactionType.EXPENSE, restored.getBudgets().get("Отдельная").getCategory().getType());
    }

    @Test
    void rejectsDuplicateCategoriesIgnoringCaseAndEdgeWhitespace() throws IOException {
        Path file = directory.resolve("users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        storage.save(List.of(sampleUser("alice")));
        ObjectNode json = (ObjectNode) mapper.readTree(file.toFile());
        ArrayNode categories = (ArrayNode) json.at("/users/0/wallet/categories");
        ObjectNode duplicate = categories.get(1).deepCopy();
        duplicate.put("name", "  еДа\t");
        categories.add(duplicate);
        mapper.writeValue(file.toFile(), json);
        String contents = Files.readString(file);

        IOException error = assertThrows(IOException.class, storage::load);
        assertTrue(error.getMessage().contains("уже существует"));
        assertEquals(contents, Files.readString(file));
    }

    @Test
    void matchesBudgetAndTransactionNamesToSavedCategory() throws IOException {
        String referenceName = "  ЕДА\t";
        Path file = directory.resolve("users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        User original = sampleUser("alice");
        storage.save(List.of(original));
        ObjectNode json = (ObjectNode) mapper.readTree(file.toFile());
        ((ObjectNode) json.at("/users/0/wallet/categories/1")).put("name", "  Еда\t");
        ((ObjectNode) json.at("/users/0/wallet/budgets/0")).put("categoryName", referenceName);
        ((ObjectNode) json.at("/users/0/wallet/transactions/1")).put("category", referenceName);
        mapper.writeValue(file.toFile(), json);

        Wallet restored = storage.load().getFirst().getWallet();
        assertEquals(2, restored.getCategories().size());
        Category category = restored.findCategory(referenceName, TransactionType.EXPENSE);
        assertEquals("Еда", category.getName());
        assertSame(category, restored.getBudgets().get("Еда").getCategory());
        assertEquals(original.getWallet().getBalance(), restored.getBalance());
        assertEquals(original.getWallet().getTotalExpenses(), restored.getExpensesByCategory(referenceName));
        assertEquals(original.getWallet().getRemainingBudget("Еда"), restored.getRemainingBudget(referenceName));
        assertEquals(ORIGINAL_DATE, restored.getTransactions().getFirst().getCreatedAt());
    }

    @Test
    void repeatedLoadsReplaceUsersWithoutDuplicatingTransactionsAndKeepUsersSeparate() throws IOException {
        JsonFileStorage storage = new JsonFileStorage(directory.resolve("users.json"));
        storage.save(List.of(sampleUser("alice"), emptyUser("bob")));
        AuthService auth = new AuthService();
        auth.load(storage);
        auth.login("alice", PASSWORD);
        auth.load(storage);

        assertNull(auth.getCurrentUser());
        assertEquals(2, auth.getUsers().size());
        Wallet alice = auth.getUsers().get("alice").getWallet();
        Wallet bob = auth.getUsers().get("bob").getWallet();
        assertEquals(2, alice.getTransactions().size());
        assertEquals(sampleUser("alice").getWallet().getBalance(), alice.getBalance());
        assertTrue(bob.getTransactions().isEmpty());
        assertTrue(bob.getCategories().isEmpty());
        assertTrue(bob.getBudgets().isEmpty());
        bob.addTransaction(new Transaction(BigDecimal.ONE, TransactionType.INCOME, "Подарок"));
        assertEquals(2, alice.getTransactions().size());
        assertEquals(BigDecimal.ONE, bob.getBalance());
        assertTrue(storage.load().get(1).getWallet().getTransactions().isEmpty());
    }

    @Test
    void absentFileReturnsEmptyStateWithoutCreatingAnything() throws IOException {
        Path file = directory.resolve("absent/users.json");
        assertTrue(new JsonFileStorage(file).load().isEmpty());
        assertFalse(Files.exists(file.getParent()));
    }

    @Test
    void savesEmptyStateAndReplacesExistingFile() throws IOException {
        JsonFileStorage storage = new JsonFileStorage(directory.resolve("users.json"));
        storage.save(List.of(sampleUser("alice")));
        storage.save(List.of());
        assertTrue(storage.load().isEmpty());
        assertNoTemporaryFiles(directory);
    }

    @Test
    void rejectsMalformedJsonWithoutChangingFile() throws IOException {
        String contents = "{broken";
        Path file = directory.resolve("users.json");
        Files.writeString(file, contents);
        assertThrows(IOException.class, () -> new JsonFileStorage(file).load());
        assertEquals(contents, Files.readString(file));
    }

    @Test
    void refusesMissingFieldsAndUnknownFields() throws IOException {
        Path file = directory.resolve("users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        storage.save(List.of(sampleUser("alice")));
        ObjectNode json = (ObjectNode) mapper.readTree(file.toFile());
        ((ObjectNode) json.at("/users/0/wallet")).remove("transactions");
        mapper.writeValue(file.toFile(), json);
        assertThrows(IOException.class, storage::load);

        storage.save(List.of(sampleUser("alice")));
        json = (ObjectNode) mapper.readTree(file.toFile());
        ((ObjectNode) json.at("/users/0")).put("password", "legacy plaintext");
        mapper.writeValue(file.toFile(), json);
        assertThrows(IOException.class, storage::load);
    }

    @Test
    void rejectsDuplicateLoginsAndBudgets() throws IOException {
        Path file = directory.resolve("users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        storage.save(List.of(sampleUser("alice")));
        ObjectNode json = (ObjectNode) mapper.readTree(file.toFile());
        ArrayNode users = (ArrayNode) json.get("users");
        users.add(users.get(0).deepCopy());
        mapper.writeValue(file.toFile(), json);
        assertThrows(IOException.class, storage::load);

        users.remove(1);
        ArrayNode budgets = (ArrayNode) json.at("/users/0/wallet/budgets");
        budgets.add(budgets.get(0).deepCopy());
        mapper.writeValue(file.toFile(), json);
        assertThrows(IOException.class, storage::load);
    }

    @Test
    void invalidSecondUserDoesNotPartiallyReplaceAuthState() throws IOException {
        Path file = directory.resolve("users.json");
        JsonFileStorage storage = new JsonFileStorage(file);
        storage.save(List.of(sampleUser("alice")));
        AuthService auth = new AuthService();
        auth.load(storage);
        auth.login("alice", PASSWORD);
        User current = auth.getCurrentUser();

        storage.save(List.of(emptyUser("bob"), sampleUser("broken")));
        ObjectNode json = (ObjectNode) mapper.readTree(file.toFile());
        ((ObjectNode) json.at("/users/1/wallet/transactions/0")).put("amount", "-10");
        mapper.writeValue(file.toFile(), json);

        assertThrows(IOException.class, () -> auth.load(storage));
        assertEquals(1, auth.getUsers().size());
        assertSame(current, auth.getUsers().get("alice"));
        assertSame(current, auth.getCurrentUser());
        assertEquals(2, current.getWallet().getTransactions().size());
    }

    @Test
    void failedReplacementCleansTemporaryFileAndPreservesExistingTarget() throws IOException {
        Path target = Files.createDirectory(directory.resolve("target.json"));
        Path marker = target.resolve("keep.txt");
        Files.writeString(marker, "keep");
        JsonFileStorage storage = new JsonFileStorage(target);

        assertThrows(IOException.class, () -> storage.save(List.of(emptyUser("alice"))));
        assertEquals("keep", Files.readString(marker));
        assertNoTemporaryFiles(directory);
        assertThrows(IOException.class, storage::load);
    }

    private static User sampleUser(String login) {
        User user = emptyUser(login);
        Wallet wallet = user.getWallet();
        Category income = new Category(TransactionType.INCOME, "Зарплата");
        Category expense = new Category(TransactionType.EXPENSE, "Еда");
        wallet.addCategory(income);
        wallet.addCategory(expense);
        wallet.addBudget(new Budget(expense, new BigDecimal("20.00")));
        wallet.addTransaction(new Transaction(new BigDecimal("123456789012345678901234567890.123456789"),
                TransactionType.INCOME, income.getName(), ORIGINAL_DATE));
        wallet.addTransaction(new Transaction(new BigDecimal("25.010000000000000000000000000001"),
                TransactionType.EXPENSE, expense.getName(), ORIGINAL_DATE.plusDays(1)));
        return user;
    }

    private static User emptyUser(String login) {
        return User.restore(login, credentials.getPasswordHash(), credentials.getPasswordSalt(), new Wallet());
    }

    private void assertNoTemporaryFiles(Path parent) throws IOException {
        try (Stream<Path> paths = Files.list(parent)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}

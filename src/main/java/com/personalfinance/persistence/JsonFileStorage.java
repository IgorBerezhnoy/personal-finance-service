package com.personalfinance.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.personalfinance.model.Budget;
import com.personalfinance.model.Category;
import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.User;
import com.personalfinance.model.Wallet;
import com.personalfinance.persistence.dto.BudgetData;
import com.personalfinance.persistence.dto.CategoryData;
import com.personalfinance.persistence.dto.StorageData;
import com.personalfinance.persistence.dto.TransactionData;
import com.personalfinance.persistence.dto.UserData;
import com.personalfinance.persistence.dto.WalletData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JsonFileStorage {
    private static final int FORMAT_VERSION = 1;

    private final Path path;
    private final ObjectMapper mapper;

    public JsonFileStorage(Path path) {
        this.path = Objects.requireNonNull(path, "Путь не может быть null").toAbsolutePath();
        this.mapper = createMapper();
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = JsonMapper.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build();
        // Денежные значения и даты должны приходить именно строками JSON.
        mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        return mapper;
    }

    public void save(Collection<User> users) throws IOException {
        Objects.requireNonNull(users, "Пользователи не могут быть null");
        List<UserData> data = new ArrayList<>();
        Set<String> logins = new HashSet<>();
        for (User user : users) {
            Objects.requireNonNull(user, "Пользователь не может быть null");
            if (!logins.add(user.getLogin())) {
                throw new IllegalArgumentException("Повторяющийся логин: " + user.getLogin());
            }
            data.add(toData(user));
        }

        Files.createDirectories(path.getParent());
        // Тот же каталог обеспечивает перемещение в пределах одной файловой системы.
        Path temporary = Files.createTempFile(path.getParent(), ".personal-finance-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValue(output, new StorageData(FORMAT_VERSION, data));
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public List<User> load() throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            StorageData data = mapper.readValue(input, StorageData.class);
            requireData(data, "корневая структура");
            if (data.version() != FORMAT_VERSION) {
                throw new IllegalArgumentException("Неподдерживаемая версия формата: " + data.version());
            }
            List<User> restored = new ArrayList<>();
            Set<String> logins = new HashSet<>();
            for (UserData userData : data.users()) {
                User user = restoreUser(requireData(userData, "пользователь"));
                if (!logins.add(user.getLogin())) {
                    throw new IllegalArgumentException("Повторяющийся логин: " + user.getLogin());
                }
                restored.add(user);
            }
            // Объекты доступны вызывающему коду только после проверки всего файла.
            return List.copyOf(restored);
        } catch (NoSuchFileException e) {
            return List.of();
        } catch (JsonProcessingException | IllegalArgumentException | DateTimeParseException e) {
            throw new IOException("Некорректные данные в JSON-файле " + path + ": " + e.getMessage(), e);
        }
    }

    private UserData toData(User user) {
        Wallet wallet = user.getWallet();
        List<TransactionData> transactions = wallet.getTransactions().stream()
                .map(transaction -> new TransactionData(transaction.getAmount().toString(),
                        transaction.getType(), transaction.getCategory(), transaction.getCreatedAt().toString()))
                .toList();
        List<CategoryData> categories = wallet.getCategories().stream()
                .map(category -> new CategoryData(category.getName(), category.getType()))
                .toList();
        List<BudgetData> budgets = wallet.getBudgets().values().stream()
                .map(budget -> new BudgetData(budget.getCategory().getName(), budget.getLimit().toString()))
                .toList();
        return new UserData(user.getLogin(), user.getPasswordHash(), user.getPasswordSalt(),
                new WalletData(transactions, categories, budgets));
    }

    private User restoreUser(UserData data) {
        return User.restore(data.login(), data.passwordHash(), data.passwordSalt(), restoreWallet(data.wallet()));
    }

    private Wallet restoreWallet(WalletData data) {
        // Jackson уже проверил обязательные поля DTO; отдельно проверяем элементы списков.
        Wallet wallet = new Wallet();
        for (CategoryData category : data.categories()) {
            requireData(category, "категория");
            wallet.addCategory(new Category(category.type(), category.name()));
        }
        for (BudgetData budget : data.budgets()) {
            requireData(budget, "бюджет");
            Category category = wallet.findCategory(budget.categoryName(), TransactionType.EXPENSE);
            if (category == null) {
                throw new IllegalArgumentException(
                        "Категория расходов '" + budget.categoryName() + "' для бюджета не найдена");
            }
            wallet.addBudget(new Budget(category, new BigDecimal(budget.limit())));
        }
        for (TransactionData transaction : data.transactions()) {
            requireData(transaction, "операция");
            wallet.addTransaction(new Transaction(
                    new BigDecimal(transaction.amount()),
                    transaction.type(), transaction.category(),
                    LocalDateTime.parse(transaction.createdAt())));
        }
        return wallet;
    }

    private static <T> T requireData(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("Отсутствуют данные: " + name);
        }
        return value;
    }
}

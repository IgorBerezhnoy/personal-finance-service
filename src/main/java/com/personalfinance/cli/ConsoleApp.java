package com.personalfinance.cli;

import com.personalfinance.model.Transaction;
import com.personalfinance.model.TransactionType;
import com.personalfinance.model.User;
import com.personalfinance.model.Wallet;
import com.personalfinance.persistence.JsonFileStorage;
import com.personalfinance.service.AuthService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ConsoleApp {
    private final Scanner scanner;
    private final AuthService authService;
    private final JsonFileStorage storage;
    private boolean isRunning;

    public ConsoleApp(AuthService authService, JsonFileStorage storage) {
        this.scanner = new Scanner(System.in);
        this.authService = authService;
        this.storage = storage;
        isRunning = true;
    }

    public void run() throws IOException {
        authService.load(storage);

        System.out.println("Добро пожаловать в Personal Finance Service!");
        System.out.println("Введите 'help' для списка команд.");

        while (isRunning) {
            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String command = scanner.nextLine().trim();
            try {
                handleCommand(command);
            } catch (NoSuchElementException e) {
                isRunning = false;
            }
        }

        authService.save(storage);
        System.out.println("Данные сохранены. До свидания!");
    }

    private void handleCommand(String command) {
        switch (command) {
            case "help" -> printHelp();
            case "register" -> handleRegister();
            case "login" -> handleLogin();
            case "logout" -> handleLogout();
            case "balance" -> handleShowBalance();
            case "income" -> handleAddTransaction(TransactionType.INCOME);
            case "expense" -> handleAddTransaction(TransactionType.EXPENSE);
            case "transactions" -> handleShowTransactions();

            case "exit" -> {
                System.out.println("Завершение программы...");
                isRunning = false;
            }

            case "" -> {
                // Пустую команду игнорируем.
            }

            default -> System.out.println("Неизвестная команда: " + command);
        }
    }

    private void handleRegister() {
        System.out.println("Введите ваши данные для регистрации:");
        System.out.println("Введите имя пользователя:");
        String username = scanner.nextLine().trim();

        System.out.println("Введите пароль:");
        String password = scanner.nextLine();

        try {
            authService.register(username, password);
            System.out.println("Регистрация прошла успешно.");
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка регистрации: " + e.getMessage());
        }
    }

    private void handleLogin() {
        System.out.println("Введите имя пользователя:");
        String username = scanner.nextLine().trim();

        System.out.println("Введите пароль:");
        String password = scanner.nextLine();

        try {
            authService.login(username, password);
            System.out.println("Вы успешно вошли в учётную запись.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Ошибка входа: " + e.getMessage());
        }
    }

    private void handleLogout() {
        if (authService.getCurrentUser() == null) {
            System.out.println("Вы не авторизованы.");
            return;
        }

        authService.logout();
        System.out.println("Вы успешно вышли из учётной записи.");
    }

    private void handleShowBalance() {
        User user = authService.getCurrentUser();
        if (user == null) {
            System.out.println("Сначала войдите в учётную запись");
            return;
        }

        System.out.println("Текущий баланс: " + user.getWallet().getBalance().toPlainString());
    }

    private void handleAddTransaction(TransactionType type) {
        User user = authService.getCurrentUser();

        if (user == null) {
            System.out.println("Сначала войдите в учётную запись.");
            return;
        }

        String operation = type == TransactionType.INCOME ? "дохода" : "расхода";
        System.out.println("Добавление " + operation);

        System.out.print("Введите сумму: ");
        String amountInput = scanner.nextLine().trim();

        BigDecimal amount;

        try {
            amount = new BigDecimal(amountInput);
        } catch (NumberFormatException e) {
            System.out.println("Ошибка: сумма должна быть числом.");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Ошибка: сумма должна быть больше нуля.");
            return;
        }

        System.out.print("Введите категорию: ");
        String categoryName = scanner.nextLine().trim();

        if (categoryName.isBlank()) {
            System.out.println("Ошибка: категория не может быть пустой.");
            return;
        }

        Wallet wallet = user.getWallet();

        boolean categoryExists = wallet.getCategories().stream()
                .anyMatch(category ->
                        category.getType() == type
                                && category.getName().trim().equalsIgnoreCase(categoryName)
                );

        if (!categoryExists) {
            System.out.println("Ошибка: такой категории нет. Сначала создайте категорию.");
            return;
        }

        try {
            Transaction transaction = new Transaction(amount, type, categoryName);
            wallet.addTransaction(transaction);

            System.out.println("Операция успешно добавлена!");
            System.out.println("Текущий баланс: " + wallet.getBalance());

            if (type == TransactionType.EXPENSE
                    && wallet.getBudgets().containsKey(categoryName)
                    && wallet.isBudgetExceeded(categoryName)) {
                System.out.println("Внимание: превышен бюджет категории " + categoryName);
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private void handleShowTransactions() {
        User user = authService.getCurrentUser();

        if (user == null) {
            System.out.println("Сначала войдите в учётную запись.");
            return;
        }

        List<Transaction> transactions = user.getWallet().getTransactions();

        if (transactions.isEmpty()) {
            System.out.println("История операций пока пуста.");
            return;
        }

        System.out.println("История операций:");

        for (int i = 0; i < transactions.size(); i++) {
            Transaction transaction = transactions.get(i);

            String type = transaction.getType() == TransactionType.INCOME
                    ? "Доход"
                    : "Расход";

            System.out.printf(
                    "%d. %s | %s | %s | %s%n",
                    i + 1,
                    type,
                    transaction.getAmount(),
                    transaction.getCategory(),
                    transaction.getCreatedAt().format(
                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    )
            );
        }
    }

    private static void printHelp() {
        System.out.println("""
                Доступные команды:
                  help         - Показать список команд и их назначение
                  register     - Создать новую учётную запись
                  login        - Войти в существующую учётную запись
                  logout       - Выйти из текущей учётной записи
                  balance      - Посмотреть текущий баланс кошелька
                  income       - Добавить доход
                  expense      - Добавить расход
                  transactions - Показать историю операций
                  exit         - Сохранить данные и завершить программу
                """);
    }

}

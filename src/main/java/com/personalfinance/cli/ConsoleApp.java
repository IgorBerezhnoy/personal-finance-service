package com.personalfinance.cli;

import com.personalfinance.model.User;
import com.personalfinance.persistence.JsonFileStorage;
import com.personalfinance.service.AuthService;

import java.io.IOException;
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

    private static void printHelp() {
        System.out.println("""
                Доступные команды:
                  help      - Показать список команд и их назначение
                  register  - Создать новую учётную запись
                  login     - Войти в существующую учётную запись
                  logout    - Выйти из текущей учётной записи
                  balance   - Посмотреть текущий баланс кошелька
                  exit      - Сохранить данные и завершить программу
                """);
    }

}

package com.personalfinance.cli;

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
            handleCommand(command);
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
        System.out.println("Регистрация нового пользователя...");
    }

    private void handleLogin() {
        System.out.println("Вход в существующую учётную запись...");
    }

    private void handleLogout() {
        System.out.println("Выход из текущей учётной записи...");
    }

    private void handleShowBalance() {
        System.out.println("Просмотр текущего баланса кошелька...");
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

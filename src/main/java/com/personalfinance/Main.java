package com.personalfinance;

import com.personalfinance.cli.ConsoleApp;
import com.personalfinance.persistence.JsonFileStorage;
import com.personalfinance.service.AuthService;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        AuthService authService = new AuthService();
        JsonFileStorage storage = new JsonFileStorage(Path.of("data/users.json"));

        ConsoleApp app = new ConsoleApp(authService, storage);
        try {
            app.run();
        } catch (IOException e) {
            System.err.println("Ошибка загрузки или сохранения данных: " + e.getMessage());
        }
    }
}

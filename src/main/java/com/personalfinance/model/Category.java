package com.personalfinance.model;

import java.util.Locale;

public class Category {
    private final String name;
    private final TransactionType type;

    public Category(TransactionType type, String name) {
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

        this.type = type;
        this.name = name.strip();
    }


    public TransactionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        return name.strip().toLowerCase(Locale.ROOT);
    }

}

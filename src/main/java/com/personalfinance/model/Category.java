package com.personalfinance.model;

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


}

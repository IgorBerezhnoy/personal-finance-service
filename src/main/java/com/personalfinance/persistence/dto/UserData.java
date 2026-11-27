package com.personalfinance.persistence.dto;

public record UserData(String login, String passwordHash, String passwordSalt, WalletData wallet) {
}

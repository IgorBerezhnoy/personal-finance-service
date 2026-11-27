package com.personalfinance.persistence.dto;

import java.util.List;

public record StorageData(int version, List<UserData> users) {
}

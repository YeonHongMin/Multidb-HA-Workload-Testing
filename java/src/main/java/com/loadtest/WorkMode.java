package com.loadtest;

/**
 * 작업 모드 정의
 */
public enum WorkMode {
    FULL("full"),               // INSERT -> COMMIT -> SELECT (기본)
    INSERT_ONLY("insert-only"), // INSERT -> COMMIT만
    SELECT_ONLY("select-only"), // SELECT만 (기존 데이터 필요)
    UPDATE_ONLY("update-only"), // UPDATE만 (기존 데이터 필요)
    DELETE_ONLY("delete-only"), // DELETE만 (기존 데이터 필요)
    MIXED("mixed");             // INSERT/UPDATE/DELETE/SELECT 혼합

    private final String value;

    WorkMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WorkMode fromString(String text) {
        for (WorkMode mode : WorkMode.values()) {
            if (mode.value.equalsIgnoreCase(text)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown work mode: " + text);
    }
}

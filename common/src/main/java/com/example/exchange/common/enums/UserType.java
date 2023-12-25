package com.example.exchange.common.enums;

public enum UserType {
    DEBT(1),

    TRADER(0);

    /**
     * User id
     */
    private final long userId;

    public long getInternalUserId() {
        return this.userId;
    }

    UserType(long userId) {
        this.userId = userId;
    }
}
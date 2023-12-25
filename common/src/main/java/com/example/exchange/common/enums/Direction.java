package com.example.exchange.common.enums;

public enum Direction {
    BUY(1),
    SELL(0);

    public final int value;

    Direction(int value) {
        this.value = value;
    }

    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    public static Direction of(int value) {
        if (1 == value) {
            return BUY;
        }
        if (0 == value) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid Direction Value.");
    }
}

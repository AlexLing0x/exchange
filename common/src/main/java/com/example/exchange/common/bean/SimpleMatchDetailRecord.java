package com.example.exchange.common.bean;

import com.example.exchange.common.enums.MatchType;

import java.math.BigDecimal;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}

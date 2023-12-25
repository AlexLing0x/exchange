package com.example.exchange.match;

import com.example.exchange.common.model.trade.OrderEntity;

import java.math.BigDecimal;

/**
 *
 * @param price
 * @param quantity
 * @param takerOrder 吃单：正在处理的订单
 * @param makerOrder 挂单：挂在买卖盘的订单
 */
public record MatchDetailRecord(BigDecimal price,BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}

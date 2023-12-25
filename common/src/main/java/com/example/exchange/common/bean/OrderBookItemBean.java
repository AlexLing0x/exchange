package com.example.exchange.common.bean;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderBookItemBean {

    public BigDecimal price;
    public BigDecimal quantity;

    public OrderBookItemBean(BigDecimal price, BigDecimal quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public void addQuantity(BigDecimal quantity) {
        this.quantity = this.getQuantity().add(quantity);
    }
}

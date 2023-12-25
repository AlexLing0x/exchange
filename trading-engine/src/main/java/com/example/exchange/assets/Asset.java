package com.example.exchange.assets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Asset {
    /**
     *  可用余额
     */
    BigDecimal available;
    /**
     *冻结余额
     */
    BigDecimal frozen;

    public Asset() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    @JsonIgnore
    public BigDecimal getTotal() {
        return available.add(frozen);
    }

    @Override
    public String toString() {
        return String.format("[available=%04.2f, frozen=%02.2f]", available, frozen);
    }
}

package com.example.exchange.match;

import java.math.BigDecimal;

public record OrderKey(Long sequenceId, BigDecimal price){

}

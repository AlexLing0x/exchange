package com.example.exchange.model.quotation;

import com.example.exchange.model.support.AbstractBarEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {
}

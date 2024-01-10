package com.example.exchange.message;

import com.example.exchange.model.quotation.TickEntity;

import java.util.List;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;
}

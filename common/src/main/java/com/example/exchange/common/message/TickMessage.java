package com.example.exchange.common.message;

import com.example.exchange.common.model.quotation.TickEntity;

import java.util.List;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;
}

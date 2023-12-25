package com.example.exchange.common.messageing;

import com.example.exchange.common.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface BatchMessageHandler <T extends AbstractMessage> {

    void processMessages(List<T> messages);
}

package com.example.exchange.common.messageing;

import com.example.exchange.common.message.AbstractMessage;

import java.util.List;

@FunctionalInterface
public interface MessageProducer <T extends AbstractMessage> {
    void sendMessage(T message);

    default void sendMessages(List<T> messages) {
        for (T message : messages) {
            sendMessage(message);
        }
    }
}

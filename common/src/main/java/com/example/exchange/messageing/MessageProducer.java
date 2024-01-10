package com.example.exchange.messageing;

import com.example.exchange.message.AbstractMessage;

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

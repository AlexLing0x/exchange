package com.example.exchange.tradingapi.service;

import com.example.exchange.common.message.event.AbstractEvent;
import com.example.exchange.common.messageing.MessageProducer;
import com.example.exchange.common.messageing.Messaging;
import com.example.exchange.common.messageing.MessagingFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendEventService {
    @Autowired
    private MessagingFactory messagingFactory;
    private MessageProducer<AbstractEvent> messageProducer;

    @PostConstruct
    public void init() {
        this.messageProducer = messagingFactory.createMessageProducer(Messaging.Topic.SEQUENCE, AbstractEvent.class);
    }

    public void sendMessage(AbstractEvent message) {
        this.messageProducer.sendMessage(message);
    }
}

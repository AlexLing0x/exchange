package com.example.exchange.store;

import com.example.exchange.common.db.DbTemplate;
import com.example.exchange.common.message.event.AbstractEvent;
import com.example.exchange.common.messageing.MessageTypes;
import com.example.exchange.common.model.trade.EventEntity;
import com.example.exchange.common.model.support.EntitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional
public class StoreService {
    @Autowired
    MessageTypes messageTypes;

    @Autowired
    DbTemplate dbTemplate;

    public List<AbstractEvent> loadEventsFromDb(long lastEventId){
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class).where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId").limit(100000).list();
        return events.stream().map(event -> (AbstractEvent)messageTypes.deserialize(event.data)).collect(Collectors.toList());
    }
    public void insertIgnore(List<? extends EntitySupport> list) {
        dbTemplate.insertIgnore(list);
    }

}

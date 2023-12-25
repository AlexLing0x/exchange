package com.example.exchange.quotation.service;

import com.example.exchange.common.enums.BarType;
import com.example.exchange.common.message.AbstractMessage;
import com.example.exchange.common.message.TickMessage;
import com.example.exchange.common.messageing.MessageConsumer;
import com.example.exchange.common.messageing.Messaging;
import com.example.exchange.common.messageing.MessagingFactory;
import com.example.exchange.common.model.quotation.*;
import com.example.exchange.common.model.support.AbstractBarEntity;
import com.example.exchange.common.redis.RedisCache;
import com.example.exchange.common.redis.RedisService;
import com.example.exchange.common.support.LoggerSupport;
import com.example.exchange.common.util.IpUtil;
import com.example.exchange.common.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;

@Component
public class QuotationService extends LoggerSupport {
    @Autowired
    ZoneId zoneId;
    @Autowired
    private RedisService redisService;
    @Autowired
    private MessagingFactory messagingFactory;
    @Autowired
    private QuotationDbService quotationDbService;

    private MessageConsumer tickConsumer;

    private String shaUpdateRecentTicksLua = null;

    private String shaUpdateBarLua = null;

    // track last processed sequence id:
    private long sequenceId;

    @PostConstruct
    public void init() {
        //init redis
        this.shaUpdateRecentTicksLua = this.redisService.loadScriptFromClassPath("/redis/update-recent-ticks.lua");
        this.shaUpdateBarLua = this.redisService.loadScriptFromClassPath("/redis/update-bar.lua");

        //init mq
        String groupId = Messaging.Topic.TICK.name() + "_" + IpUtil.getHostId();
        this.tickConsumer = messagingFactory.createBatchMessageListener(Messaging.Topic.TICK, groupId,
                this::processMessages);
    }

    @PreDestroy
    public void shutdown() {
        if (this.tickConsumer != null) {
            this.tickConsumer.stop();
            this.tickConsumer = null;
        }
    }

    //处理接受的消息
    private void processMessages(List<AbstractMessage> messages) {
        for (AbstractMessage message : messages) {
            processMessages((TickMessage) message);
        }
    }

    //处理一个Tick消息
    private void processMessages(TickMessage message) {
        //忽略重复信息
        if (message.sequenceId < this.sequenceId) {
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("process ticks: sequenceId = {}, {} ticks...", message.sequenceId, message.ticks.size());
        }

        // 生成[tick, tick...]列表以及合并为一个Bar:
        this.sequenceId = message.sequenceId;
        final long createdAt = message.createdAt;
        StringJoiner ticksStrJoiner = new StringJoiner(",", "[", "]");
        StringJoiner ticksJoiner = new StringJoiner(",", "[", "]");
        BigDecimal openPrice = BigDecimal.ZERO;
        BigDecimal closePrice = BigDecimal.ZERO;
        BigDecimal highPrice = BigDecimal.ZERO;
        BigDecimal lowPrice = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.ZERO;

        for (TickEntity tick : message.ticks) {
            String json = tick.toJson();
            ticksStrJoiner.add("\"" + json + "\"");
            ticksJoiner.add(json);
            if (openPrice.signum() == 0) {
                openPrice = tick.price;
                closePrice = tick.price;
                highPrice = tick.price;
                lowPrice = tick.price;
            } else {
                // open price is set:
                closePrice = tick.price;
                highPrice = highPrice.max(tick.price);
                lowPrice = lowPrice.min(tick.price);
            }
            quantity = quantity.add(tick.quantity);
        }
        // 计算应该合并的每种类型的Bar的开始时间:
        long sec = createdAt / 1000;
        long min = sec / 60;
        long hour = min / 60;
        long secStartTime = sec * 1000; // 秒K的开始时间
        long minStartTime = min * 60 * 1000; // 分钟K的开始时间
        long hourStartTime = hour * 3600 * 1000; // 小时K的开始时间
        long dayStartTime = Instant.ofEpochMilli(hourStartTime).atZone(zoneId).withHour(0).toEpochSecond() * 1000; // 日K的开始时间，与TimeZone相关

        //更新redis最近的Ticks缓存
        String ticksData = ticksJoiner.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("generated ticks data: {}", ticksData);
        }
        Boolean tickOk = redisService.executeScriptReturnBoolean(this.shaUpdateRecentTicksLua,
                new String[]{RedisCache.Key.RECENT_TICKS},
                new String[]{String.valueOf(this.sequenceId), ticksData, ticksStrJoiner.toString()});
        if (!tickOk.booleanValue()) {
            logger.warn("ticks are ignored by Redis.");
            return;
        }
        //保存tick至数据库
        this.quotationDbService.saveTicks(message.ticks);

        // 更新各种类型的K线:
        String strCreatedBars = redisService.executeScriptReturnString(this.shaUpdateBarLua,
                new String[]{RedisCache.Key.SEC_BARS, RedisCache.Key.MIN_BARS, RedisCache.Key.HOUR_BARS, RedisCache.Key.DAY_BARS},
                new String[]{
                        String.valueOf(this.sequenceId), // sequence id
                        String.valueOf(secStartTime), // sec-start-time
                        String.valueOf(minStartTime), // min-start-time
                        String.valueOf(hourStartTime), // hour-start-time
                        String.valueOf(dayStartTime), // day-start-time
                        String.valueOf(openPrice), // open
                        String.valueOf(highPrice), // high
                        String.valueOf(lowPrice), // low
                        String.valueOf(closePrice), // close
                        String.valueOf(quantity) // quantity
                });
        logger.info("returned created bars: " + strCreatedBars);

        //将Redis返回的K线保存至数据库
        Map<BarType, BigDecimal[]> barMap = JsonUtil.readJson(strCreatedBars, TYPE_BARS);
        if (!barMap.isEmpty()) {
            //创建Bar
            SecBarEntity secBar = createBar(SecBarEntity::new, barMap.get(BarType.SEC));
            MinBarEntity minBar = createBar(MinBarEntity::new, barMap.get(BarType.MIN));
            HourBarEntity hourBar = createBar(HourBarEntity::new, barMap.get(BarType.HOUR));
            DayBarEntity dayBar = createBar(DayBarEntity::new, barMap.get(BarType.DAY));
            this.quotationDbService.saveBars(secBar, minBar, hourBar, dayBar);
        }

    }

    private <T extends AbstractBarEntity> T createBar(Supplier<T> fn, BigDecimal[] data) {
        if (data == null) {
            return null;
        }
        T t = fn.get();
        t.startTime = data[0].longValue();
        t.openPrice = data[1];
        t.highPrice = data[2];
        t.lowPrice = data[3];
        t.closePrice = data[4];
        t.quantity = data[5];
        return t;
    }

    private static final TypeReference<Map<BarType, BigDecimal[]>> TYPE_BARS = new TypeReference<>() {
    };

}
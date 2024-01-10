package com.example.exchange.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.redis")
@Data
public class RedisConfiguration {

    private String host;

    private int port;

    private String password;

    private int database;


}

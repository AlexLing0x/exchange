package com.example.exchange.support;

import com.example.exchange.db.DbTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDbService extends LoggerSupport{

    @Autowired
    protected DbTemplate db;
}

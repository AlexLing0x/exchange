package com.example.exchange.common.support;

import com.example.exchange.common.db.DbTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDbService extends LoggerSupport{

    @Autowired
    protected DbTemplate db;
}

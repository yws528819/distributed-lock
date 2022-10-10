package com.yws.com.yws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    @Autowired
    private StringRedisTemplate redisTemplate;
}

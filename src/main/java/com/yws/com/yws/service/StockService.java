package com.yws.com.yws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void deduct() {
        //1.查询库存信息
        String stock = redisTemplate.opsForValue().get("stock");

        //2.判断库存是否充足
        if (stock != null && stock.length() > 0) {
            Integer st = Integer.valueOf(stock);
            if (st > 0) {
                //3.扣减库存
                redisTemplate.opsForValue().set("stock", String.valueOf(--st));
            }
        }
    }

}

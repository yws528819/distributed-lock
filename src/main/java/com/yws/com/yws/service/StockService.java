package com.yws.com.yws.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void deduct() {
        String uuid = UUID.randomUUID().toString();
        //尝试加锁，不存在才加锁
        while (!redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS)) {
            //加锁失败，睡一会，再获取锁
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
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
        } finally {
            //先判断是否是自己的锁，再解锁（使用lua脚本）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                            "then " +
                            "   redis.call('del', KEYS[1]) " +
                            "else " +
                            "   return 0 " +
                            "end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
            //if (StringUtils.equals(redisTemplate.opsForValue().get("lock"), uuid)) {
            //    redisTemplate.delete("lock");
            //}
        }
    }



    /**
     * redis乐观锁
     */
    public void deduct1() {

        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //watch
                operations.watch("stock");

                //1.查询库存信息
                String stock = redisTemplate.opsForValue().get("stock");

                //2.判断库存是否充足
                if (stock != null && stock.length() > 0) {
                    Integer st = Integer.valueOf(stock);
                    if (st > 0) {
                        //multi
                        operations.multi();
                        //3.扣减库存
                        redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                    }
                }
                //exec 执行事务
                List exec = operations.exec();
                //如果执行事务的返回结果为空，则代表扣减库存失败，重试
                if (exec == null || exec.size() == 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    deduct();
                }

                return exec;
            }
        });
    }

}

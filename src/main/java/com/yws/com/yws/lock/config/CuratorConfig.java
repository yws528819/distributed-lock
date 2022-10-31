package com.yws.com.yws.lock.config;


import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CuratorConfig {

    @Bean
    public CuratorFramework curatorFramework() {
        //初始化一个重试策略。这里使用的是指数补偿策略。初始间隔时间，重试次数
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(10000, 3);
        //初始化curator客户端
        CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.0.105:2181", retryPolicy);
        client.start();//手动启动，否则很多方法或者功能不工作的。
        return client;
    }
}

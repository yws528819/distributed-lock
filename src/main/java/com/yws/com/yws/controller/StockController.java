package com.yws.com.yws.controller;

import com.yws.com.yws.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("/stock")
    public String deduct() {
        stockService.deduct();
        return "hello stock deduct";
    }


    @GetMapping("/fairylock/{id}")
    public String fairyLock(@PathVariable("id") String id) {
        stockService.fairyLock(id);
        return "hello test fairy lock";
    }

    @GetMapping("/readlock")
    public String readLock() {
        stockService.readLock();
        return "hello test read lock";
    }

    @GetMapping("/writelock")
    public String writeLock() {
        stockService.writeLock();
        return "hello test write lock";
    }

    @GetMapping("/semaphore")
    public String semaphore() {
        stockService.semaphore();
        return "hello test semaphore";
    }

    @GetMapping("/latch")
    public String latch() {
        stockService.latch();
        return "班长锁门了";
    }

    @GetMapping("/countdown")
    public String countDown() {
        stockService.coutDown();
        return "出来了一位同学";
    }


    @GetMapping("/zk/readlock")
    public String zkReadLock() {
        stockService.zkReadLock();
        return "zk read lock";
    }

    @GetMapping("/zk/writelock")
    public String zkWriteLock() {
        stockService.zkWriteLock();
        return "zk write lock";
    }
}

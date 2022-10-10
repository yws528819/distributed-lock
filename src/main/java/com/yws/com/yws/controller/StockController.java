package com.yws.com.yws.controller;

import com.yws.com.yws.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
}

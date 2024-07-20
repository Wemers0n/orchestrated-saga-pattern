package com.ms.orchestrated.orderservice.core.controller;

import com.ms.orchestrated.orderservice.core.document.Order;
import com.ms.orchestrated.orderservice.core.dtos.OrderRequest;
import com.ms.orchestrated.orderservice.core.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request){
        return this.service.createOrder(request);
    }
}

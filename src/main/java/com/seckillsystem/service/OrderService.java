package com.seckillsystem.service;

import com.seckillsystem.mapper.OrderMapper;
import com.seckillsystem.pojo.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    @Autowired
    private OrderMapper orderMapper;

    public void createOrder(Order order) {
        orderMapper.insert(order);
    }
}

package com.seckillsystem.service;

import com.seckillsystem.config.MyRabbitMQConfig;
import com.seckillsystem.pojo.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MQOrderService {
    @Autowired
    private OrderService orderService;

    /**
     * 监听订单消息队列，并消费
     * @param order
     */
    @RabbitListener(queues = MyRabbitMQConfig.ORDER_QUEUE)
    public void createOrder(Order order) {
        log.info("收到订单消息，订单用户为：{}，商品名称为：{}", order.getOrderUser(), order.getOrderName());

        /**
         * 调用数据库orderService创建订单信息
         */
        orderService.createOrder(order);
    }
}

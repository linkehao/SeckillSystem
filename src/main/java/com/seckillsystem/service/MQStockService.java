package com.seckillsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.seckillsystem.config.MyRabbitMQConfig;
import com.seckillsystem.pojo.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MQStockService {
    @Autowired
    private StockService stockService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 监听库存消息队列，并消费
     */
    @RabbitListener(queues = MyRabbitMQConfig.STORY_QUEUE)
    @Transactional
    public void decrByStock(Message message, Channel channel) {

        try {
            //设置预抓取总数
            try {
                channel.basicQos(300);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String body = new String(message.getBody());
            HashMap<String, String> map = null;
            try {
                map = objectMapper.readValue(body, HashMap.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }


            String stockName = map.get("stockName");
            String username = map.get("username");
            log.info("库存消息队列收到的消息商品信息是：{}", stockName);
            /**
             * 调用数据库service给数据库对应商品库存减一
             */
            stockService.decrByStock(stockName);
            log.info("收到订单消息，订单用户为：{}，商品名称为：{}", username, stockName);

            Order order = new Order();
            order.setOrderName(username);
            order.setOrderUser(username);
            /**
             * 调用数据库orderService创建订单信息
             */
            orderService.createOrder(order);

            /**
             * 第一个参数:消息的唯一标识
             * 第二个参数:是否开启批处理
             */
//            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }

//                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            try {
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            throw e;
        }


    }
}

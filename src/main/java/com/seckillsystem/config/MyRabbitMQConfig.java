package com.seckillsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckillsystem.service.RedisService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
public class MyRabbitMQConfig {
    // 库存订单交换机
    public static final String STORY_ORDER_EXCHANGE = "STORY_ORDER_EXCHANGE";

    // 库存订单队列
    public static final String STORY_ORDER_QUEUE = "STORY_ORDER_QUEUE";

    // 库存订单路由键
    public static final String STORY_ORDER_ROUTING_KEY = "STORY_ORDER_ROUTING_KEY";

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(factory);
        rabbitTemplate.setMessageConverter(messageConverter());

        // 消息是否成功发送到Exchange
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @SneakyThrows
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                String body = new String(correlationData.getReturned().getMessage().getBody());
                Map map = objectMapper.readValue(body, Map.class);
                if (ack) {
                    log.info("消息成功发送到Exchange");
                } else {
                    log.info("消息发送到Exchange失败", correlationData, cause);
                    // 发送到交换机失败，恢复redis数据
                    redisService.increBy((String) map.get("stockName"));
                    // 用户参与失败，去掉参与成功的记录以便再次参与
                    redisService.delete((String) map.get("username"));
                }
            }
        });

        // 触发setReturnCallback回调必须设置mandatory=true, 否则Exchange没有找到Queue就会丢弃掉消息, 而不会触发回调
        rabbitTemplate.setMandatory(true);
        // 消息是否从Exchange路由到Queue, 注意: 这是一个失败回调, 只有消息从Exchange路由到Queue失败才会回调这个方法
        rabbitTemplate.setReturnsCallback(new RabbitTemplate.ReturnsCallback() {
            @SneakyThrows
            @Override
            public void returnedMessage(ReturnedMessage returnedMessage) {
                String body = new String(returnedMessage.getMessage().getBody());
                Map map = objectMapper.readValue(body, Map.class);
                // 交换机发送到队列失败，恢复redis数据
                redisService.increBy((String) map.get("stockName"));
                System.out.println("交换机到队列失败：" + map.get("stockName"));
                // 用户参与失败，去掉参与成功的记录以便再次参与
                redisService.delete((String) map.get("username"));
            }
        });

        return rabbitTemplate;
    }

    // 创建库存订单交换机
    @Bean
    public Exchange getStoryOrderExchange() {
        return ExchangeBuilder.directExchange(STORY_ORDER_EXCHANGE).durable(true).build();
    }

    //创建库存队列
    @Bean
    public Queue getStoryOrderQueue() {
        return new Queue(STORY_ORDER_QUEUE, true);
    }

    //库存交换机和库存队列绑定
    @Bean
    public Binding bindStory() {
        return BindingBuilder.bind(getStoryOrderQueue()).to(getStoryOrderExchange()).with(STORY_ORDER_ROUTING_KEY).noargs();
    }
}

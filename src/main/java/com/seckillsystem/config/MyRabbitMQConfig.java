package com.seckillsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckillsystem.service.RedisService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
public class MyRabbitMQConfig {
    //库存交换机
    public static final String STORY_EXCHANGE = "STORY_EXCHANGE";

    //订单交换机
    public static final String ORDER_EXCHANGE = "ORDER_EXCHANGE";

    //库存队列
    public static final String STORY_QUEUE = "STORY_QUEUE";

    //订单队列
    public static final String ORDER_QUEUE = "ORDER_QUEUE";

    //库存路由键
    public static final String STORY_ROUTING_KEY = "STORY_ROUTING_KEY";

    //订单路由键
    public static final String ORDER_ROUTING_KEY = "ORDER_ROUTING_KEY";

//    @Autowired
//    private ConnectionFactory connectionFactory;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 重写RabbitListenerContainerFactory，指定手动提交，否则在配置文件配了手动ack也不生效
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        // 指定手动提交
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
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
//                System.out.println(correlationData);
//                System.out.println(correlationData.getReturned().getMessage().getMessageProperties());

                String body = new String(correlationData.getReturned().getMessage().getBody());
                Map map = objectMapper.readValue(body, Map.class);

//                System.out.println(map.get("stockName"));
//                System.out.println(map.get("username"));
                if (ack) {
                    log.info("消息成功发送到Exchange");
                } else {
                    log.info("消息发送到Exchange失败", correlationData, cause);
                    // 发送到交换机失败，恢复redis数据
//                System.out.println(message);
                redisService.increBy((String) map.get("stockName"));
                }
            }
        });

//                (correlationData, ack, cause) -> {
//            System.out.println(correlationData);
//            if (ack) {
//                log.info("消息成功发送到Exchange");
//            } else {
//                log.info("消息发送到Exchange失败", correlationData, cause);
//                // 发送到交换机失败，回滚redis数据
////                System.out.println(message);
////                redisService.increBy(message);
//            }
//        });

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
//                redisService.increBy((String) map.get("stockName"));
                System.out.println("交换机到队列失败：" + map.get("stockName"));
            }
        });

        return rabbitTemplate;
    }

    //创建库存交换机
    @Bean
    public Exchange getStoryExchange() {
        return ExchangeBuilder.directExchange(STORY_EXCHANGE).durable(true).build();
    }

    //创建库存队列
    @Bean
    public Queue getStoryQueue() {
        return new Queue(STORY_QUEUE, true);
    }

    //库存交换机和库存队列绑定
    @Bean
    public Binding bindStory() {
        return BindingBuilder.bind(getStoryQueue()).to(getStoryExchange()).with(STORY_ROUTING_KEY).noargs();
    }

    //创建订单队列
    @Bean
    public Queue getOrderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    //创建订单交换机
    @Bean
    public Exchange getOrderExchange() {
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE).durable(true).build();
    }

    //订单队列与订单交换机进行绑定
    @Bean
    public Binding bindOrder() {
        return BindingBuilder.bind(getOrderQueue()).to(getOrderExchange()).with(ORDER_ROUTING_KEY).noargs();
    }
}

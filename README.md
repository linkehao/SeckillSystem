# SeckillSystem
秒杀系统

技术架构：Springboot、Redis、RabbitMQ

关键技术实现说明

1、通过redis的单线程实现用户限流

```java
 // 限制每个用户只能下一单
Long count = redisService.increBy(username);
if (count > 1) {
     log.info("用户：{}已参与过这次秒杀，请勿再次参与", username);
     return "用户" + username + "已参与过这次秒杀，请勿再次参与";
 }
```



2、启动comfirm机制和手动ACK

```java
#确认消息已发送到交换机，选择确认类型为交互
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
# 设置手动确认(ack)
spring.rabbitmq.listener.simple.acknowledge-mode=manual
```



3、覆盖RabbitTemplate的Bean对象，重写ConfirmCallback和ReturnsCallback方法

使用jackson的ObjectMapper反序列化消息，失败的话要回退redis数据，并且去掉用户参与记录，以便用户再次参与秒杀（因为还没有发送到MQ，所以用户没有成功参与秒杀，所以不算下单）

```java
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
```



4、使用jackson的ObjectMapper把map进行序列化，转成bytes数组组装到Message，然后再进行发送消息

记得要把消息set到CorrelationData里面，否则重写的ConfirmCallback里面获取不到Message信息

```java
/**
 * 说明该商品的库存量有剩余，可以进行下订单操作
        */
log.info("用户：{}秒杀该商品：{}库存有余，可以进行下订单操作", username, stockName);
HashMap<String, String> map = new HashMap<>();
map.put("stockName", stockName);
map.put("username", username);
//发消息给库存消息队列，将库存数据减一
Message message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(map)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
CorrelationData correlationData = new CorrelationData();
correlationData.setReturnedMessage(message);
rabbitTemplate.convertAndSend(MyRabbitMQConfig.STORY_ORDER_EXCHANGE, MyRabbitMQConfig.STORY_ORDER_ROUTING_KEY, map, correlationData);
info = "用户" + username + "秒杀" + stockName + "成功";
```



5、把减库存 和 添加订单信息 放到一个事务里

使用手动ack来保证消息一定被消费

```java
/**
 * 监听库存消息队列，并消费
 */
@RabbitListener(queues = MyRabbitMQConfig.STORY_ORDER_QUEUE)
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
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
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
```












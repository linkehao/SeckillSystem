package com.seckillsystem;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SeckillsystemApplicationTests {

    @Test
    void contextLoads() {

    }

    /**
     * 获取连接
     * @return Connection
     * @throws Exception
     */
    public static Connection getConnection() throws Exception {
        //定义连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(15672);
        //设置vhost
        factory.setVirtualHost("/tzb");
        factory.setUsername("test");
        factory.setPassword("123456");
        //通过工厂获取连接
        Connection connection = factory.newConnection();
        return connection;
    }

    //创建队列，发送消息
    public static void main(String[] args) throws Exception {
        String QUEUE_NAME = "";
        //获取连接
        Connection connection = getConnection();
        //创建通道
        Channel channel = connection.createChannel();
        //声明创建队列
        channel.queueDeclare(QUEUE_NAME,false,false,false,null);
        //消息内容
        String message = "Hello World!";
        channel.basicPublish("",QUEUE_NAME,null,message.getBytes());
        System.out.println("发送消息："+message);
        //关闭连接和通道
        channel.close();
        connection.close();
    }

}

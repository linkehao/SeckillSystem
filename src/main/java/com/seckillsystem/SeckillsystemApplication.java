package com.seckillsystem;

import com.seckillsystem.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
public class SeckillsystemApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(SeckillsystemApplication.class, args);
    }

    @Autowired
    private RedisService redisService;

    /**
     * redis初始化各商品的库存量
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        redisService.put("watch", 100, 20);
    }

//    //设置redistemplate的序列化
//    @Bean
//    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        // 1.创建 redisTemplate 模版
//        RedisTemplate<Object, Object> template = new RedisTemplate<>();
//        // 2.关联 redisConnectionFactory
//        template.setConnectionFactory(redisConnectionFactory);
//        // 3.创建 序列化类
//        GenericToStringSerializer genericToStringSerializer = new GenericToStringSerializer(Object.class);
//        // 6.序列化类，对象映射设置
//        // 7.设置 value 的转化格式和 key 的转化格式
//        template.setValueSerializer(genericToStringSerializer);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.afterPropertiesSet();
//        return template;
//    }

}

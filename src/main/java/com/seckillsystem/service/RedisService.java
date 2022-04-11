package com.seckillsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置String键值对
     * @param key
     * @param value
     * @param millis
     */
    public void put(String key, Object value, long millis) {
        redisTemplate.opsForValue().set(key, value, millis, TimeUnit.MINUTES);
    }

    public void putForHash(String objectKey, String hkey, String value) {
        redisTemplate.opsForHash().put(objectKey, hkey, value);
    }

    /**
     * 对指定key的键值减一
     * @param key
     * @return
     */
    public Long decrBy(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    public Long increBy(String key) {
       return  redisTemplate.opsForValue().increment(key);
    }

    /**
     * 获取指定key的值
     * @param key
     * @return
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除key
     * @param key
     * @return
     */
    public Object delete(String key) {
        return redisTemplate.delete(key);
    }

}

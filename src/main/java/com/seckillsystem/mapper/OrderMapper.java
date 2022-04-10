package com.seckillsystem.mapper;

import com.seckillsystem.pojo.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    Integer insert(Order order);
}

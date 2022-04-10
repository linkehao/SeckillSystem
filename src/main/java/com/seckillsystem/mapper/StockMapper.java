package com.seckillsystem.mapper;

import com.seckillsystem.pojo.Stock;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockMapper {
    List<Stock> selectList(@Param("name") String name);

    Integer updateByPrimaryKey(Stock stock);
}

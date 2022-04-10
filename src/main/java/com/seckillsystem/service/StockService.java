package com.seckillsystem.service;

import com.seckillsystem.mapper.StockMapper;
import com.seckillsystem.pojo.Stock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class StockService {
    @Autowired
    private StockMapper stockMapper;

    public void decrByStock(String stockName) {
        synchronized(this) {
            List<Stock> stocks = stockMapper.selectList(stockName);
            if (!CollectionUtils.isEmpty(stocks)) {
                Stock stock = stocks.get(0);
                stock.setStock(stock.getStock() - 1);
                stockMapper.updateByPrimaryKey(stock);
            }
        }
    }

    public Integer selectByName(String stockName) {
        synchronized (this){
            List<Stock> stocks = stockMapper.selectList(stockName);
            if (!CollectionUtils.isEmpty(stocks)) {
                return stocks.get(0).getStock().intValue();
            }
            return 0;
        }
    }
}

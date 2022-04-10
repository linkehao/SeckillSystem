package com.seckillsystem.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Stock implements Serializable {
    private static final long serialVersionUID = 6235666939721331057L;
    Integer id;
    String name;
    Integer stock;
}
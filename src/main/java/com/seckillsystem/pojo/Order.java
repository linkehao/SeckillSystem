package com.seckillsystem.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class Order implements Serializable {
    private static final long serialVersionUID = -8271355836132430489L;
    Integer id;
    String orderName;
    String orderUser;
}

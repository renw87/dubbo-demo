package com.renwei.service;

import com.renwei.demo.HelloService;
import com.renwei.dubbo.annotation.RpcAnnotation;

import java.text.SimpleDateFormat;
import java.util.Date;

@RpcAnnotation(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name);
        return "Hello " + name;
    }
}

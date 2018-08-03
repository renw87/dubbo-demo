package com.renwei.dubbo.demo.provider;


import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Provider {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-demo-provider.xml"});
        context.start();

        System.in.read(); // 按任意键退出
    }

}

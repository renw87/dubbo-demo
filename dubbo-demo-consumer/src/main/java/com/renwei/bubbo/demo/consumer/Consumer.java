package com.renwei.bubbo.demo.consumer;


import com.renwei.dubbo.demo.DemoService;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {

    //启动的时候需要设置VM参数：-Djava.net.preferIPv4Stack=true
    //否则会报错：java.net.SocketException: Can't assign requested address
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-demo-consumer.xml"});
        context.start();

        DemoService demoService = (DemoService) context.getBean("demoService");
        String res = demoService.sayHelllo("renwei");
        System.out.println(res);
    }
}

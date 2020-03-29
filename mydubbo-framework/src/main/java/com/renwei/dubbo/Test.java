package com.renwei.dubbo;

import com.renwei.dubbo.registry.RegistryService;
import com.renwei.dubbo.registry.zookeeper.ZookeeperRegistry;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        //注册中心Test
        RegistryService registryService = new ZookeeperRegistry();
        String serviceName = "com.renwei.demo.HelloService";
        registryService.registry(serviceName, "127.0.0.1:7788");
        String address = registryService.discover(serviceName);
        System.out.println(address);
        //System.in.read();

    }
}

package com.renwei;

import com.renwei.demo.HelloService;
import com.renwei.dubbo.registry.RegistryService;
import com.renwei.dubbo.registry.zookeeper.ZookeeperRegistry;
import com.renwei.dubbo.rpc.RpcClientProxy;

public class Consumer {

    public static void main(String[] args) {
        RegistryService registryService = new ZookeeperRegistry();
        RpcClientProxy client = new RpcClientProxy(registryService);
        HelloService helloService = client.create(HelloService.class);
        System.out.println(helloService.sayHello("renwei"));
        System.out.println(helloService.sayHello("任伟"));
    }
}

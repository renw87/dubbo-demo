package com.renwei;

import com.renwei.demo.HelloService;
import com.renwei.dubbo.registry.RegistryService;
import com.renwei.dubbo.registry.zookeeper.ZookeeperRegistry;
import com.renwei.dubbo.rpc.NettyRpcServer;
import com.renwei.service.HelloServiceImpl;

public class Provider {
    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        /*RegistryService registryService = new ZookeeperRegistry();
        NettyRpcServer server = new NettyRpcServer(registryService, "127.0.0.1:8888");
        server.bind(helloService);
        server.publisher();*/
        new NettyRpcServer("com.renwei", "127.0.0.1:8888");
    }
}

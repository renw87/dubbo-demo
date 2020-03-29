package com.renwei.dubbo.rpc;

import com.renwei.dubbo.annotation.RpcAnnotation;
import com.renwei.dubbo.registry.RegistryService;
import com.renwei.dubbo.registry.zookeeper.ZookeeperRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NettyRpcServer {
    private RegistryService     registryService;
    private String              serviceAddress;
    private Map<String, Object> handlerMap = new HashMap<>(16);
    List<Class<?>> rpcClassList = new ArrayList<>();

    public NettyRpcServer(RegistryService registryService, String serviceAddress) {
        this.registryService = registryService;
        this.serviceAddress = serviceAddress;
    }

    public NettyRpcServer(String basePackage, String serviceAddress) {
        registryService = new ZookeeperRegistry();
        this.serviceAddress = serviceAddress;
        scannerClass(basePackage);
        doRegister();
        startServer();
    }

    /**
     * 发布服务
     */
    public void publisher() {
        for (String serviceName : handlerMap.keySet()) {
            registryService.registry(serviceName, serviceAddress);
        }
        startServer();
    }

    private void startServer() {
        try {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            //启动netty服务
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    ChannelPipeline channelPipeline = channel.pipeline();
                    //channelPipeline.addLast(new ObjectDecoder(1024 * 1024, ClassResolvers
                    // .weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                    //channelPipeline.addLast(new ObjectEncoder());
                    //自定义协议解码器
                    /** 入参有5个，分别解释如下
                     maxFrameLength：框架的最大长度。如果帧的长度大于此值，则将抛出TooLongFrameException。
                     lengthFieldOffset：长度字段的偏移量：即对应的长度字段在整个消息数据中得位置
                     lengthFieldLength：长度字段的长度。如：长度字段是int型表示，那么这个值就是4（long型就是8）
                     lengthAdjustment：要添加到长度字段值的补偿值
                     initialBytesToStrip：从解码帧中去除的第一个字节数
                     */
                    channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4,
                            0, 4));
                    channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    channelPipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE,
                            ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
                    channelPipeline.addLast("encoder", new ObjectEncoder());
                    channelPipeline.addLast(new RpcServerHandler(handlerMap));
                }
            }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
            String[] addr = serviceAddress.split(":");
            String ip = addr[0];
            int port = Integer.valueOf(addr[1]);
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            System.out.println("服务启动，成功。");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 服务名称和子类对象一一对应起来
     *
     * @param services 对象实现类
     */
    public void bind(Object... services) {
        //将实现类通过注解获取实现类的名称、实现类的实现放入map集合中。
        for (Object service : services) {
            //得到服务名称
            RpcAnnotation annotation = service.getClass().getAnnotation(RpcAnnotation.class);
            if (annotation != null) {
                String serviceName = annotation.value().getName();
                //客户端最终需要根据服务名称调用子类对象的实现
                handlerMap.put(serviceName, service);
            }
        }
    }

    private void scannerClass(String basePackage) {
        URL url = this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            //如果是一个文件夹，继续递归
            if (file.isDirectory()) {
                scannerClass(basePackage + "." + file.getName());
            } else {
                String className = basePackage + "." + file.getName().replace(".class", "").trim();
                try {
                    Class<?> clazz = Class.forName(className);
                    RpcAnnotation annotation = clazz.getAnnotation(RpcAnnotation.class);
                    if(annotation != null) {
                        rpcClassList.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 完成注册
     */
    private void doRegister() {
        if (rpcClassList.size() == 0) {
            return;
        }
        for (Class<?> clazz : rpcClassList) {
            try {
                Class<?> i = clazz.getInterfaces()[0];
                handlerMap.put(i.getName(), clazz.newInstance());
                registryService.registry(i.getName(), serviceAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

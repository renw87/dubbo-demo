package com.renwei.dubbo.rpc;

import com.renwei.dubbo.annotation.RpcAnnotation;
import com.renwei.dubbo.registry.RegistryService;
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

import java.util.HashMap;
import java.util.Map;

public class NettyRpcServer {
    private RegistryService     registryService;
    private String              serviceAddress;
    private Map<String, Object> handlerMap = new HashMap<>(16);

    public NettyRpcServer(RegistryService registryService, String serviceAddress) {
        this.registryService = registryService;
        this.serviceAddress = serviceAddress;
    }

    /**
     * 发布服务
     */
    public void publisher() {
        for (String serviceName : handlerMap.keySet()) {
            registryService.registry(serviceName, serviceAddress);
        }
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
                    //channelPipeline.addLast(new ObjectDecoder(1024 * 1024, ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                    //channelPipeline.addLast(new ObjectEncoder());
                    //自定义协议解码器
                    /** 入参有5个，分别解释如下
                     maxFrameLength：框架的最大长度。如果帧的长度大于此值，则将抛出TooLongFrameException。
                     lengthFieldOffset：长度字段的偏移量：即对应的长度字段在整个消息数据中得位置
                     lengthFieldLength：长度字段的长度。如：长度字段是int型表示，那么这个值就是4（long型就是8）
                     lengthAdjustment：要添加到长度字段值的补偿值
                     initialBytesToStrip：从解码帧中去除的第一个字节数
                     */
                    channelPipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4, 0, 4));
                    channelPipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    channelPipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
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
     * 子对象的实现
     *
     * @param services 对象实现类
     */
    public void bind(Object... services) {
        //将实现类通过注解获取实现类的名称、实现类的实现放入map集合中。
        for (Object service : services) {
            RpcAnnotation annotation = service.getClass().getAnnotation(RpcAnnotation.class);
            if(annotation != null) {
                String serviceName = annotation.value().getName();
                handlerMap.put(serviceName, service);
            }
        }
    }
}

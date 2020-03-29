package com.renwei.dubbo.rpc;

import com.renwei.dubbo.bean.RpcRequest;
import com.renwei.dubbo.registry.RegistryService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcClientProxy {

    private RegistryService registryService;

    public RpcClientProxy(RegistryService registryService) {
        this.registryService = registryService;
    }

    public <T> T create(final Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass}, new InvocationHandler() {
                    //封装RpcRequest请求对象，然后通过netty发送给服务等
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        RpcRequest rpcRequest = new RpcRequest();
                        rpcRequest.setClassName(method.getDeclaringClass().getName());
                        rpcRequest.setMethodName(method.getName());
                        rpcRequest.setTypes(method.getParameterTypes());
                        rpcRequest.setParams(args);
                        //服务发现，zk进行通讯
                        String serviceName = interfaceClass.getName();
                        //获取服务实现url地址
                        String serviceAddress = registryService.discover(serviceName);
                        //解析ip和port
                        System.out.println("服务端实现地址：" + serviceAddress);
                        String[] arrs = serviceAddress.split(":");
                        String host = arrs[0];
                        int port = Integer.parseInt(arrs[1]);
                        System.out.println("服务实现ip:" + host);
                        System.out.println("服务实现port:" + port);
                        final RpcProxyHandler rpcProxyHandler = new RpcProxyHandler();
                        //通过netty方式进行连接发送数据
                        EventLoopGroup group = new NioEventLoopGroup();
                        try {
                            Bootstrap bootstrap = new Bootstrap();
                            bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY,
                                    true)
                                    .handler(new ChannelInitializer<SocketChannel>() {
                                        @Override
                                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                                            ChannelPipeline channelPipeline = socketChannel.pipeline();
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
                                            //netty实现代码
                                            channelPipeline.addLast(rpcProxyHandler);
                                        }
                                    });
                            ChannelFuture future = bootstrap.connect(host, port).sync();
                            //将封装好的对象写入
                            future.channel().writeAndFlush(rpcRequest);
                            future.channel().closeFuture().sync();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            group.shutdownGracefully();
                        }
                        return rpcProxyHandler.getResponse();
                    }
                });
    }
}

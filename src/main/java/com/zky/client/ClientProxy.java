package com.zky.client;


import com.zky.handler.ClientInHandler;
import com.zky.initializer.ProtoBuffChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;


public class ClientProxy {

    public static AttributeKey<Object> port = AttributeKey.newInstance("port");
    public static AttributeKey<Object> uuid = AttributeKey.newInstance("uuid");

    public static void main(String[] args) {
        for (String port : ClientInHandler.reflectionLists.keySet()) {
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup).
                        channel(NioSocketChannel.class).
                        option(ChannelOption.AUTO_READ,true).
                        handler(new ProtoBuffChannelInitializer(new ClientInHandler()));
                ChannelFuture f = bootstrap.connect("localhost", 8088).sync();
                SocketChannel channel = (SocketChannel) f.channel();
                channel.attr(AttributeKey.valueOf("port")).set(port);
                System.out.println("client start done");
                ChannelFuture channelFuture = f.channel().closeFuture();
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        System.out.println(port+"服务关闭");
                        workerGroup.shutdownGracefully();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

}

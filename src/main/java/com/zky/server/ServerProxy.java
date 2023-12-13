package com.zky.server;


import com.zky.handler.ServerHandler;
import com.zky.initializer.ByteChannelInitializer;
import com.zky.initializer.ProtoBuffChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ServerProxy {
    public static Map<Integer,ServerSocketChannel> hasOutServerSocketChannel = new ConcurrentHashMap<>();
    public static NioEventLoopGroup parentGroup = new NioEventLoopGroup();
    public static NioEventLoopGroup childGroup = new NioEventLoopGroup();
    public static AttributeKey<Object> port = AttributeKey.newInstance("port");
    public static AttributeKey<Object> uuid = AttributeKey.newInstance("uuid");

    public static void main(String[] args)  {


        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup,childGroup).
                    channel(NioServerSocketChannel.class).
                    option(ChannelOption.SO_BACKLOG,128).
                    childHandler(new ProtoBuffChannelInitializer(new ServerHandler()));
            ChannelFuture f = b.bind(8088).sync();
            System.out.println("8088 server start done");
            ChannelFuture channelFuture = f.channel().closeFuture();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    System.out.println("8088服务下线");
                    childGroup.shutdownGracefully();
                    parentGroup.shutdownGracefully();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}

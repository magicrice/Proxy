package com.zky.server;


import com.zky.initializer.ClientServerChannelInitializer;
import com.zky.initializer.OutChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;


public class ServerProxy {

    public static void main(String[] args)  {
        NioEventLoopGroup parentGroup = new NioEventLoopGroup();
        NioEventLoopGroup childGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup,childGroup).
                    channel(NioServerSocketChannel.class).
                    option(ChannelOption.SO_BACKLOG,128).
                    childHandler(new OutChannelInitializer());
            ChannelFuture f = b.bind(9999).sync();
            System.out.println("server start done");
            ChannelFuture channelFuture = f.channel().closeFuture();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    System.out.println("9999服务下线");
                    childGroup.shutdownGracefully();
                    parentGroup.shutdownGracefully();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup,childGroup).
                    channel(NioServerSocketChannel.class).
                    option(ChannelOption.SO_BACKLOG,128).
                    childHandler(new ClientServerChannelInitializer());
            ChannelFuture f = b.bind(8088).sync();
            System.out.println("server start done");
            ChannelFuture channelFuture = f.channel().closeFuture();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    System.out.println("9999服务下线");
                    childGroup.shutdownGracefully();
                    parentGroup.shutdownGracefully();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}

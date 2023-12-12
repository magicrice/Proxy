package com.zky.client;


import com.zky.initializer.ClientServerChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientProxy {

    public static void main(String[] args) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup).
                    channel(NioSocketChannel.class).
                    option(ChannelOption.AUTO_READ,true).
                    handler(new ClientServerChannelInitializer());
            ChannelFuture f = bootstrap.connect("localhost", 8088).sync();
            System.out.println("client start done");
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            workerGroup.shutdownGracefully();
        }

    }

}

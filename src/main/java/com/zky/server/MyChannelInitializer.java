package com.zky.server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class MyChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        System.out.println("链接报告开始");
        System.out.println("链接报告信息：有一客户端链接到本服务端");
        System.out.println("链接报告ip："+channel.localAddress().getHostString());
        System.out.println("链接报告port:"+channel.localAddress().getPort());
        System.out.println("链接报告完闭");
        channel.pipeline().addLast(new MyServerHandler());
    }
}

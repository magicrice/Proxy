package com.zky.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当客户端主动连接服务端后，这个通道活跃，可以传输数据
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        System.out.println("链接报告开始");
        System.out.println("链接报告信息：有一客户端链接到本服务端");
        System.out.println("链接报告ip："+channel.localAddress().getHostString());
        System.out.println("链接报告port:"+channel.localAddress().getPort());
        System.out.println("链接报告完闭");
        //通知客户端连接建立成功
        String str = "通知客户端连接建立成功"+" "+ new Date()+" "+channel.localAddress().getHostString()+"\r\n";

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收msg消息
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" 接收到消息:"+msg);

    }


    /**
     * 处理完服务器返回的数据时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    /**
     * 发生异常时调用
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}

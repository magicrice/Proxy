package com.zky.handler;

import com.zky.server.ServerChannelHandler;
import com.zky.utils.MsgUtil;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
        if(channel.localAddress().getPort() == 8088){
            ServerChannelHandler.channelGroup.add(channel);
        }else {
            String uuid = UUID.randomUUID().toString();
            DefaultChannelGroup defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            defaultChannelGroup.add(channel);
            ServerChannelHandler.channelGroupMap.put(uuid,defaultChannelGroup);
            if(AttributeKey.exists("uuid")){
                channel.attr(AttributeKey.valueOf("uuid")).set(uuid);
            }else {
                channel.attr(AttributeKey.newInstance("uuid")).set(uuid);
            }
            //通知client连接被代理端
//            ServerChannelHandler.channelGroup.write(("create-"+uuid+"-end").getBytes(StandardCharsets.UTF_8));
//            ServerChannelHandler.channelGroup.write(("\r\n").getBytes(StandardCharsets.UTF_8));
//            ServerChannelHandler.channelGroup.flush();
            ServerChannelHandler.channelGroup.writeAndFlush(MsgUtil.buildMsg(uuid,0,null));
        }

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端断开连接"+ctx.channel().localAddress().toString());
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if(ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))){
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ServerChannelHandler.channelGroupMap.remove(uuid);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收msg消息
        SocketChannel channel = (SocketChannel) ctx.channel();
        if(channel.localAddress().getPort() == 9999){
            System.out.println("请求信息:"+new String((byte[]) msg));
            String uuid = (String) channel.attr(AttributeKey.valueOf("uuid")).get();
//            ServerChannelHandler.channelGroup.write(("create-"+uuid+"-end").getBytes(StandardCharsets.UTF_8));
//            ServerChannelHandler.channelGroup.write(("\r\n").getBytes(StandardCharsets.UTF_8));
//            ServerChannelHandler.channelGroup.writeAndFlush(msg);
            ServerChannelHandler.channelGroup.writeAndFlush(MsgUtil.buildMsg(uuid,((byte[]) msg).length,(byte[])msg));

        }else {
            ByteBuffer buffer = ByteBuffer.wrap((byte[]) msg);
            byte[] headByte = new byte[49];
            buffer.get(headByte,0,headByte.length);
            byte[] tailByte = null;
            System.out.println("被代理返回信息:"+new String(buffer.array()));
            if(buffer.array().length != headByte.length){
                tailByte = new byte[buffer.array().length-49];
                buffer.get(tailByte);
            }
            String[] split = new String(headByte).split("\r\n");
            String uuid = split[0].replaceAll("create-", "").replaceAll("-end", "");
            if(tailByte != null){
                ServerChannelHandler.channelGroupMap.get(uuid).writeAndFlush(tailByte);
            }
        }

    }


    /**
     * 处理完服务器返回的数据时调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("信息处理完毕");
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
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if(ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))){
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ServerChannelHandler.channelGroupMap.remove(uuid);
        }
        ctx.close();
        System.out.println("异常信息:\r\n"+cause.getMessage());
    }
}

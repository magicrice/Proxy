package com.zky.handler;

import com.zky.initializer.ByteChannelInitializer;
import com.zky.server.ServerProxy;
import com.zky.utils.MsgInfo;
import com.zky.server.ServerChannelHandler;
import com.zky.utils.MsgUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.UUID;

@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当客户端主动连接服务端后，这个通道活跃，可以传输数据
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        System.out.println("链接报告开始");
        System.out.println("链接报告信息：有一客户端链接到本服务端");
        System.out.println("链接报告ip：" + channel.localAddress().getHostString());
        System.out.println("链接报告port:" + channel.localAddress().getPort());
        System.out.println("链接报告完闭");
        //通知客户端连接建立成功
        if (channel.localAddress().getPort() == 8088) {
//            ServerChannelHandler.channelGroup.put();add(channel);
        } else {
            DefaultChannelGroup defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            defaultChannelGroup.add(channel);
            String uuid = UUID.randomUUID().toString();
            String port = String.valueOf(channel.localAddress().getPort());
            ServerChannelHandler.channelGroupMap.put(uuid, defaultChannelGroup);
            channel.attr(AttributeKey.valueOf("uuid")).set(uuid);
            channel.attr(AttributeKey.valueOf("port")).set(port);
            //通知client连接被代理端
            ServerChannelHandler.channelGroup.get(port).writeAndFlush(MsgUtil.buildMsg(2, uuid, port, 0, null));
        }

    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端断开连接" + ctx.channel().localAddress().toString());
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if (ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))) {
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ServerChannelHandler.channelGroupMap.remove(uuid);
        }
        if (ctxChannel.localAddress().getPort() == 8088) {
            String port = (String) ctxChannel.attr(AttributeKey.valueOf("port")).get();
            System.out.println("端口:"+port);
            ServerProxy.hasOutServerSocketChannel.get(port).close();
            ServerProxy.hasOutServerSocketChannel.remove(port);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收msg消息
        SocketChannel channel = (SocketChannel) ctx.channel();
        System.out.println("local地址为：" + channel.localAddress().getHostString() + "->" + channel.localAddress().getPort());
        System.out.println("remote地址为：" + channel.remoteAddress().getHostString() + "->" + channel.remoteAddress().getPort());
        if (channel.localAddress().getPort() == 8088) {
            MsgInfo msgInfo = (MsgInfo) msg;
            if (3 == msgInfo.getType()) {
                System.out.println("被代理返回信息:" + new String(msgInfo.getMsgContent()));
                if (msgInfo.getSize() != 0) {
                    ServerChannelHandler.channelGroupMap.get(msgInfo.getUuid()).writeAndFlush(msgInfo.getMsgContent());
                }
            } else if (1 == msgInfo.getType()) {
                //创建对外服务
                //通道绑定ip
                channel.attr(AttributeKey.valueOf("port")).set(msgInfo.getPort());

                System.out.println("创建对外服务");
                DefaultChannelGroup defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
                defaultChannelGroup.add(channel);
                ServerChannelHandler.channelGroup.put(msgInfo.getPort(), defaultChannelGroup);
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(ServerProxy.parentGroup, ServerProxy.childGroup).
                            channel(NioServerSocketChannel.class).
                            option(ChannelOption.SO_BACKLOG, 128).
                            childHandler(new ByteChannelInitializer(new ServerHandler()));
                    ChannelFuture f = b.bind(Integer.parseInt(msgInfo.getPort())).sync();
                    System.out.println("server start done");
                    ChannelFuture channelFuture = f.channel().closeFuture();
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channelFuture.channel();
                    ServerProxy.hasOutServerSocketChannel.put(Integer.parseInt(msgInfo.getPort()), serverSocketChannel);
                    channelFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            System.out.println(msgInfo.getUuid() + "服务下线");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("请求信息:" + new String((byte[]) msg));
            String uuid = (String) channel.attr(AttributeKey.valueOf("uuid")).get();
            String port = String.valueOf(channel.localAddress().getPort());
            ServerChannelHandler.channelGroup.get(port).writeAndFlush(MsgUtil.buildMsg(3, uuid, port, ((byte[]) msg).length, (byte[]) msg));
        }

    }


    /**
     * 处理完服务器返回的数据时调用
     *
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
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if (ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))) {
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ServerChannelHandler.channelGroupMap.remove(uuid);
        }
        if (ctxChannel.localAddress().getPort() == 8088) {
            String port = (String) ctxChannel.attr(AttributeKey.valueOf("port")).get();
            ServerProxy.hasOutServerSocketChannel.get(port).close();
            ServerProxy.hasOutServerSocketChannel.remove(port);
        }
        ctx.close();
        System.out.println("异常信息:\r\n" + cause.getMessage());
    }
}

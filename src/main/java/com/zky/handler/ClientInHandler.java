package com.zky.handler;

import com.zky.client.ClientChannelHandler;
import com.zky.utils.MsgInfo;
import com.zky.initializer.ByteChannelInitializer;
import com.zky.utils.MsgUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@ChannelHandler.Sharable
public class ClientInHandler extends ChannelInboundHandlerAdapter {
    public static Map<String, String> reflectionLists = new ConcurrentHashMap<>();
    private static Map<Integer, SocketChannel> channelMap = new ConcurrentHashMap<>();

    static {
        reflectionLists.put("9999", "120.46.189.242:3306");
        reflectionLists.put("7777", "localhost:8080");
    }

    /**
     * 当客户端主动连接服务端后，这个通道活跃，可以传输数据
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        if (channel.remoteAddress().getPort() == 8088) {
            //通知服务端创建对外服务
            DefaultChannelGroup defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            defaultChannelGroup.add(channel);
            while (true){
                String port = (String) channel.attr(AttributeKey.valueOf("port")).get();
                if(port == null || "".equals(port)){
                    continue;
                }
                System.out.println("port为："+port);
                ClientChannelHandler.channelGroup.put(port, defaultChannelGroup);
                System.out.println("通知服务端创建对外服务");
                ctx.writeAndFlush(MsgUtil.buildMsg(1, null, port, 0, null));
                break;
            }
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if (ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))) {
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ClientChannelHandler.channelGroupMap.remove(uuid);
        }
        System.out.println("客户端断开连接" + ctx.channel().localAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收msg消息
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();

        if (ctxChannel.remoteAddress().getPort() == 8088) {
            MsgInfo msgInfo = (MsgInfo) msg;
            if (msgInfo.getType() == 2) {
                //创建通道
                if (!ClientChannelHandler.channelGroupMap.containsKey(msgInfo.getUuid())) {
                    String hostPort = reflectionLists.get(msgInfo.getPort());
                    String[] split = hostPort.split(":");
                    //连接被代理端
                    SocketChannel channel = null;
                    EventLoopGroup workerGroup = new NioEventLoopGroup();
                    try {
                        Bootstrap bootstrap = new Bootstrap();
                        bootstrap.group(workerGroup).
                                channel(NioSocketChannel.class).
                                option(ChannelOption.AUTO_READ, true).
                                handler(new ByteChannelInitializer(new ClientInHandler()));
                        ChannelFuture f = bootstrap.connect(split[0], Integer.parseInt(split[1])).sync();
                        channel = (SocketChannel) f.channel();
                        System.out.println("beRe start done");
                        ChannelFuture channelFuture = channel.closeFuture();
                        channelFuture.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                System.out.println("被代理服务器断开连接");
                                workerGroup.shutdownGracefully();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    DefaultChannelGroup defaultChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
                    defaultChannelGroup.add(channel);
                    channel.attr(AttributeKey.valueOf("uuid")).set(msgInfo.getUuid());
                    channel.attr(AttributeKey.valueOf("port")).set(msgInfo.getPort());
                    ClientChannelHandler.channelGroupMap.put(msgInfo.getUuid(), defaultChannelGroup);
                }
            } else if (msgInfo.getType() == 3) {
                if (msgInfo.getSize() != 0) {
                    System.out.println("发送给被代理服务器消息为" + new String(msgInfo.getMsgContent()));
                    ClientChannelHandler.channelGroupMap.get(msgInfo.getUuid()).writeAndFlush(msgInfo.getMsgContent());
                }
            }
        } else {
            //被代理端返回结果
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            String port = (String) ctxChannel.attr(AttributeKey.valueOf("port")).get();

            System.out.println("被代理端返回消息" + new String((byte[]) msg));
            ClientChannelHandler.channelGroup.get(port).writeAndFlush(MsgUtil.buildMsg(3, uuid, port, ((byte[]) msg).length, (byte[]) msg));
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
            ClientChannelHandler.channelGroupMap.remove(uuid);
        }
        ctx.close();
        cause.printStackTrace();
        System.out.println("异常信息:\r\n" + cause.getMessage());
    }
}

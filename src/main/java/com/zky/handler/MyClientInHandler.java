package com.zky.handler;

import com.zky.client.ClientChannelHandler;
import com.zky.initializer.OutChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MyClientInHandler extends ChannelInboundHandlerAdapter {
    private static String beReIp = "localhost";
    private static Integer beRePort = 8080;

    /**
     * 当客户端主动连接服务端后，这个通道活跃，可以传输数据
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel channel = (SocketChannel) ctx.channel();
        if(channel.remoteAddress().getPort() == 8088){
            ClientChannelHandler.channelGroup.add(channel);
        }
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();
        if(ctxChannel.hasAttr(AttributeKey.valueOf("uuid"))){
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();
            ClientChannelHandler.channelGroupMap.remove(uuid);
        }
        System.out.println("客户端断开连接"+ctx.channel().localAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //接收msg消息
        SocketChannel ctxChannel = (SocketChannel) ctx.channel();

        if(ctxChannel.remoteAddress().getPort() == 8088){
            ByteBuffer buffer = ByteBuffer.wrap((byte[]) msg);
            byte[] headByte = new byte[49];
            buffer.get(headByte,0,headByte.length);
            byte[] tailByte = null;
            if(buffer.array().length != headByte.length){
                tailByte = new byte[buffer.array().length-49];
                buffer.get(tailByte);
            }
            System.out.println("服务端传输消息:"+new String(buffer.array()));

            String[] split = new String(headByte).split("\r\n");
            String uuid = split[0].replaceAll("create-", "").replaceAll("-end", "");
            if(!ClientChannelHandler.channelGroupMap.containsKey(uuid)){
                //连接被代理端
                SocketChannel channel = null;
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(workerGroup).
                            channel(NioSocketChannel.class).
                            option(ChannelOption.AUTO_READ,true).
                            handler(new OutChannelInitializer());
//                    ChannelFuture f = bootstrap.connect("localhost", 8080).sync();
                    ChannelFuture f = bootstrap.connect(beReIp, beRePort).sync();
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
                if(AttributeKey.exists("uuid")){
                    channel.attr(AttributeKey.valueOf("uuid")).set(uuid);
                }else {
                    channel.attr(AttributeKey.newInstance("uuid")).set(uuid);
                }
                ClientChannelHandler.channelGroupMap.put(uuid,defaultChannelGroup);
            }
            if(tailByte != null){
                System.out.println("发送给被代理服务器消息为"+new String(tailByte));
                ClientChannelHandler.channelGroupMap.get(uuid).writeAndFlush(tailByte);
            }
        }else {
            //被代理端返回结果
            String uuid = (String) ctxChannel.attr(AttributeKey.valueOf("uuid")).get();

            System.out.println("被代理端返回消息"+new String((byte[]) msg));
            ClientChannelHandler.channelGroup.write(("create-"+uuid+"-end").getBytes(StandardCharsets.UTF_8));

            ClientChannelHandler.channelGroup.write(("\r\n").getBytes(StandardCharsets.UTF_8));
            ClientChannelHandler.channelGroup.writeAndFlush(msg);
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
            ClientChannelHandler.channelGroupMap.remove(uuid);
        }
        ctx.close();
        System.out.println("异常信息:\r\n"+cause.getMessage());
    }
}

package com.zky.initializer;

import com.zky.utils.MsgInfo;
import com.zky.utils.ObjDecoder;
import com.zky.utils.ObjEncoder;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ProtoBuffChannelInitializer extends ChannelInitializer<SocketChannel> {
    private ChannelInboundHandlerAdapter channelInboundHandlerAdapter;

    public ProtoBuffChannelInitializer(ChannelInboundHandlerAdapter channelInboundHandlerAdapter) {
        this.channelInboundHandlerAdapter = channelInboundHandlerAdapter;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
//        socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
        //基于指定字符串【换行符，这样功能等同于LineBasedFrameDecoder】
//        channel.pipeline().addLast(new DelimiterBasedFrameDecoder(1024,false, Delimiters.lineDelimiter()));
        //基于最大长度
//        channel.pipeline().addLast(new FixedLengthFrameDecoder(4));
        //解码转String,注意调整自己的编码格式GBK、UTF-8
        socketChannel.pipeline().addLast(new ObjDecoder(MsgInfo.class));
        //编码转string
        socketChannel.pipeline().addLast(new ObjEncoder(MsgInfo.class));
        //在管道中添加我们自己的接收数据实现方法
        socketChannel.pipeline().addLast(channelInboundHandlerAdapter);
    }
}

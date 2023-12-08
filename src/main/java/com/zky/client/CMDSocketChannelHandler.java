package com.zky.client;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class CMDSocketChannelHandler extends BaseClientSocketChannelHandler {
    @Override
    public void accept() throws Exception {

    }

    @Override
    public boolean read(SelectionKey sk, Selector readSelector,Selector writeSelector) throws Exception {
        boolean flag = false;
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            int n = 0;
            while (n < ClientProxy.limit) {
                int read = socketChannel.read(byteBuffer);
                if (read == -1) {
                    break;
                } else if (read == 0) {
                    n++;
                } else {
                    byteBuffer.flip();
                    //输出
                    String cmd = new String(byteBuffer.array());
                    if (cmd.startsWith("connect")) {
                        cmd = cmd.substring(0, 48);
                        String uuid = cmd.replaceAll("connect-", "").replaceAll("-end", "");
                        //创建通道 连接server
                        SocketChannel serverSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8088));
                        serverSocketChannel.configureBlocking(false);
                        //发送唯一id消息
                        serverSocketChannel.write(ByteBuffer.wrap(("connect-" + uuid + "-end").getBytes(StandardCharsets.UTF_8)));
                        System.out.println("发送创建通道信息->" + uuid);

                        serverSocketChannel.register(readSelector, SelectionKey.OP_READ,"server-");
                    }
                    byteBuffer.clear();
                    n = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sk.cancel();
        }
        return flag;
    }

    @Override
    public void write(SelectionKey sk) throws Exception {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        System.out.println("请求创建对外服务");
        socketChannel.write(ByteBuffer.wrap(("create-9999-end").getBytes(StandardCharsets.UTF_8)));

        sk.interestOps(SelectionKey.OP_READ);
    }
}

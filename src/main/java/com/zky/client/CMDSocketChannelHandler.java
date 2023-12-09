package com.zky.client;


import com.zky.basehandler.BaseClientSocketChannelHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class CMDSocketChannelHandler extends BaseClientSocketChannelHandler {

    public CMDSocketChannelHandler() {
    }

    public CMDSocketChannelHandler(List<String> list) {
        super(list);
    }

    @Override
    public void accept() throws Exception {

    }

    @Override
    public void read(SelectionKey sk) throws Exception {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            int n = 0;
            while (n < limit) {
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
                        cmd = cmd.substring(0, cmd.indexOf("-end"));
                        String uuid = cmd.replaceAll("connect-", "");
                        //创建通道 连接server
                        SocketChannel serverSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8088));
                        serverSocketChannel.configureBlocking(false);
                        //发送唯一id消息
                        serverSocketChannel.write(ByteBuffer.wrap(("connect-" + uuid + "-end").getBytes(StandardCharsets.UTF_8)));
                        System.out.println("发送创建通道信息->" + uuid);

                        serverSocketChannel.register(readSelector, SelectionKey.OP_READ, "server-"+uuid.substring(0,uuid.indexOf("-"))+"-");
                        serverSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "server-" + uuid);

                    }
                    byteBuffer.clear();
                    n = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sk.cancel();
        }
    }

    @Override
    public void write(SelectionKey sk) throws Exception {
        Set<Integer> integers = reflections.keySet();
        for (Integer integer : integers) {
            Reflection reflection = reflections.get(integer);
            if (!reflection.getFlag()) {
                SocketChannel socketChannel = (SocketChannel) sk.channel();
                System.out.println("请求创建对外服务");
                socketChannel.write(ByteBuffer.wrap(("create-" + reflection.getOutPort() + "-end").getBytes(StandardCharsets.UTF_8)));
                sk.attach(reflection.getOutPort());
                sk.interestOps(SelectionKey.OP_READ);
                reflection.setFlag(true);
                break;
            }
        }
    }
}

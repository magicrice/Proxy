package com.zky.client;


import com.zky.basehandler.BaseClientSocketChannelHandler;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

public class BeRepresentedSocketChannelHandler extends BaseClientSocketChannelHandler {

    public BeRepresentedSocketChannelHandler() {

    }

    public BeRepresentedSocketChannelHandler(List<String> list) {
        super(list);
    }

    @Override
    public void accept() throws Exception {

    }

    @Override
    public void read(SelectionKey sk) throws Exception {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        SocketChannel clientSocketChannel = null;
        String skAttachment = sk.attachment().toString();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int i = writeSelector.selectNow();
        if (i == 0) {
            Thread.sleep(100);
            return;
        }
        Iterator<SelectionKey> clientIterator = writeSelector.selectedKeys().iterator();
        if (!clientIterator.hasNext()) {
            return;
        }
        while (clientIterator.hasNext()) {
            SelectionKey clientSelectionKey = clientIterator.next();
            if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("beRepresented-", "server-"))) {
                String uuid = skAttachment.replaceAll("beRepresented-", "");
                if(clientServerChannelFlag.contains(uuid)){
                    clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                }
            }
            clientIterator.remove();
        }
        System.out.println("通道：" + skAttachment);
        if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
            System.out.println("服务端通道已关闭");
            return;
        }

        System.out.println("执行读方法");
        System.out.println(sk.attachment());
        int num = 0;
        try {
            while (num <= limit) {
                int read = socketChannel.read(byteBuffer);
                if (read == -1) {
                    sk.cancel();
                    System.out.println(skAttachment + "通道-1已关闭");
                    break;
                } else if (read == 0) {
                    num++;
                } else {
                    byteBuffer.flip();
                    //输出
                    clientSocketChannel.write(byteBuffer);
                    System.out.println(new String(byteBuffer.array()));
                    byteBuffer.clear();
                    num = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sk.cancel();
            System.out.println(skAttachment + "通道异常已关闭");
        }
    }

    @Override
    public void write(SelectionKey sk) throws Exception {

    }
}

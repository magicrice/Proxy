package com.zky.client;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class BeRepresentedSocketChannelHandler extends BaseClientSocketChannelHandler {
    @Override
    public void accept() throws Exception {

    }

    @Override
    public boolean read(SelectionKey sk, Selector readSelector, Selector writeSelector) throws Exception {
        boolean flag = false;
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        SocketChannel clientSocketChannel = null;
        String skAttachment = sk.attachment().toString();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int i = writeSelector.selectNow();
        if (i == 0) {
            Thread.sleep(100);
            return flag;
        }
        Iterator<SelectionKey> clientIterator = writeSelector.selectedKeys().iterator();
        if (!clientIterator.hasNext()) {
            return flag;
        }
        while (clientIterator.hasNext()) {
            SelectionKey clientSelectionKey = clientIterator.next();
            if (skAttachment.startsWith("beRepresented-")) {
                if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("beRepresented-", "server-"))) {
                    clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                    flag = true;
                }
            }
            clientIterator.remove();
        }
        System.out.println("通道：" + skAttachment);
        if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
            System.out.println("服务端通道已关闭");
            return flag;
        }

        System.out.println("执行读方法");
        System.out.println(sk.attachment());
        int num = 0;
        try {
            while (num <= ClientProxy.limit) {
                int read = socketChannel.read(byteBuffer);
                if (read == -1) {
                    sk.cancel();
                    //server通道关闭代表整个流程结束
                    if (skAttachment.startsWith("server-")) {
                        System.out.println(writeSelector.selectNow());
                        Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
                        while (tmpClientIterator.hasNext()) {
                            SelectionKey next = tmpClientIterator.next();
                            String uuid = skAttachment.replaceAll("beRepresented-", "");
                            if (!"".equals(uuid)) {
                                if (next.attachment().toString().endsWith(uuid)) {
                                    next.cancel();
                                    next.channel().close();
                                    System.out.println(next.attachment() + "通道关闭");
                                }
                            }
                            tmpClientIterator.remove();
                        }
                    }
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
            //server通道关闭代表整个流程结束
            System.out.println(writeSelector.selectNow());
            Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
            while (tmpClientIterator.hasNext()) {
                SelectionKey next = tmpClientIterator.next();
                String uuid = skAttachment.replaceAll("beRepresented-", "");
                if (!"".equals(uuid)) {
                    if (next.attachment().toString().endsWith(uuid)) {
                        next.cancel();
                    }
                }
                tmpClientIterator.remove();
            }
            System.out.println(skAttachment + "通道异常已关闭");
        }
        return flag;
    }

    @Override
    public void write(SelectionKey sk) throws Exception {

    }
}

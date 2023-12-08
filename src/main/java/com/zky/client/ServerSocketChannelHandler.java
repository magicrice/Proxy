package com.zky.client;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class ServerSocketChannelHandler extends BaseClientSocketChannelHandler {
    @Override
    public void accept() throws Exception {

    }

    @Override
    public boolean read(SelectionKey sk, Selector readSelector, Selector writeSelector) throws Exception {
        boolean flag = false;
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        SocketChannel clientSocketChannel = null;
        if (sk.attachment() == null || "server-".endsWith(sk.attachment().toString())) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int num = 0;
            try {
                while (num <= ClientProxy.limit) {
                    int read = socketChannel.read(byteBuffer);
                    if (read == -1) {
                        break;
                    } else if (read == 0) {
                        num++;
                    } else {
                        byteBuffer.flip();
                        //输出
                        String s = new String(byteBuffer.array());
                        if (s.startsWith("connect")) {
                            s = s.substring(0, 52);
                            String uuid = s.replaceAll("connect-", "").replaceAll("-success", "");
                            if (s.endsWith("success")) {
                                System.out.println("通道建立成功");
                                sk.attach("server-" + uuid);
                                socketChannel.register(writeSelector, SelectionKey.OP_WRITE, "server-" + uuid);
                                //创建通道 连接被代理端
                                SocketChannel beRepresentedSocketChannel = SocketChannel.open(new InetSocketAddress(ClientProxy.clientHost, ClientProxy.clientPort));
                                beRepresentedSocketChannel.configureBlocking(false);
                                beRepresentedSocketChannel.register(readSelector, SelectionKey.OP_READ, "beRepresented-" + uuid);
                                beRepresentedSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "beRepresented-" + uuid);
                                break;
                            }
                        }
                        byteBuffer.clear();
                        num = 0;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
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
                if (skAttachment.startsWith("server-")) {
                    if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("server-", "beRepresented-"))) {
                        clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                        flag = true;
                    }
                }
                clientIterator.remove();
            }
            if (skAttachment.startsWith("server-")) {
                if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
                    clientSocketChannel = SocketChannel.open(new InetSocketAddress(ClientProxy.clientHost, ClientProxy.clientPort));
                    clientSocketChannel.configureBlocking(false);
                    clientSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "beRepresented-" + skAttachment.replaceAll("server-", ""));
                    clientSocketChannel.register(readSelector, SelectionKey.OP_READ, "beRepresented-" + skAttachment.replaceAll("server-", ""));
                    flag = true;
                }
            }
            System.out.println("通道：" + skAttachment);
            if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
                System.out.println("被代理服务通道已关闭");
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
                                String uuid = skAttachment.replaceAll("server-", "");
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
                    String uuid = skAttachment.replaceAll("server-", "");
                    if (!"".equals(uuid)) {
                        if (next.attachment().toString().endsWith(uuid)) {
                            next.cancel();
                        }
                    }
                    tmpClientIterator.remove();
                }
                System.out.println(skAttachment + "通道异常已关闭");
            }
        }
        return flag;
    }

    @Override
    public void write(SelectionKey sk) throws Exception {

    }
}

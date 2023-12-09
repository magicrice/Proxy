package com.zky.server;

import com.zky.basehandler.BaseServerSocketChannelHandler;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientServerSocketChannelHandler extends BaseServerSocketChannelHandler {
    private Map<String, Boolean> map = new ConcurrentHashMap<>();

    @Override
    public void accept(SelectionKey sk) throws Exception {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
        System.out.println("执行接受方法");
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(readSelector, SelectionKey.OP_READ, "client-");
    }

    @Override
    public void read(SelectionKey sk) throws Exception {
        SocketChannel clientSocketChannel = null;
        if (sk.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) sk.channel();
            if (sk.attachment() == null || "client-".equals(sk.attachment().toString())) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int num = 0;
                try {
                    while (num <= ServerProxy.limit) {
                        int read = socketChannel.read(byteBuffer);
                        if (read == -1) {
                            break;
                        } else if (read == 0) {
                            num++;
                        } else {
                            byteBuffer.flip();
                            //输出
                            String cmd = new String(byteBuffer.array());
                            if (cmd.startsWith("connect")) {
                                if (cmd.contains("-end")) {
                                    cmd = cmd.substring(0, cmd.indexOf("-end"));
                                    String uuid = cmd.replaceAll("connect-", "");
                                    sk.attach("client-" + uuid);
                                    socketChannel.register(writeSelector, SelectionKey.OP_WRITE, "client-" + uuid);
                                    socketChannel.write(ByteBuffer.wrap(("connect-" + uuid + "-success").getBytes(StandardCharsets.UTF_8)));
                                    ServerProxy.serverClientChannelFlag.add(uuid);
                                    System.out.println("返回客户端通道建立成功");
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
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                String skAttachment = sk.attachment().toString();
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
                    if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("client-", "out-"))) {
                        clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                    }
                    clientIterator.remove();
                }
                System.out.println("执行读方法");
                int num = 0;
                try {
                    while (num <= ServerProxy.limit) {
                        int read = socketChannel.read(byteBuffer);
                        if (read == -1) {
                            sk.cancel();
                            //清除writeSelector的通道注册信息
                            System.out.println(writeSelector.selectNow());
                            Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
                            String uuid = skAttachment.replaceAll("client-", "");
                            ServerProxy.serverClientChannelFlag.remove(uuid);
                            while (tmpClientIterator.hasNext()) {
                                SelectionKey next = tmpClientIterator.next();
                                if (!"".equals(uuid)) {
                                    if (next.attachment().toString().endsWith(uuid)) {
                                        next.cancel();
                                        next.channel().close();
                                        System.out.println(next.attachment() + "通道关闭");
                                    }
                                }
                                tmpClientIterator.remove();
                            }
                            System.out.println(skAttachment + "通道-1已关闭");
                            break;
                        } else if (read == 0) {
                            num++;
                        } else {
                            byteBuffer.flip();
                            String s = new String(byteBuffer.array());
                            System.out.println(s);
                            //输出
                            clientSocketChannel.write(byteBuffer);
                            byteBuffer.clear();
                            num = 0;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sk.cancel();
                    //清除writeSelector的通道注册信息
                    System.out.println(writeSelector.selectNow());
                    Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
                    String uuid = skAttachment.replaceAll("client-", "");
                    ServerProxy.serverClientChannelFlag.remove(uuid);
                    while (tmpClientIterator.hasNext()) {
                        SelectionKey next = tmpClientIterator.next();
                        if (!"".equals(uuid)) {
                            if (next.attachment().toString().endsWith(uuid)) {
                                next.cancel();
                                next.channel().close();
                                System.out.println(next.attachment() + "通道关闭");
                            }
                        }
                        tmpClientIterator.remove();
                    }
                    System.out.println(skAttachment + "通道异常已关闭");
                }
            }
        }
    }

    @Override
    public void write() {

    }
}

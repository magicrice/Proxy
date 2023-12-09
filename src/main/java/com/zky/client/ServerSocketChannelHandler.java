package com.zky.client;


import com.zky.basehandler.BaseClientSocketChannelHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

public class ServerSocketChannelHandler extends BaseClientSocketChannelHandler {
    public ServerSocketChannelHandler() {
    }

    public ServerSocketChannelHandler(List<String> list) {
        super(list);
    }

    @Override
    public void accept() throws Exception {

    }

    @Override
    public void read(SelectionKey sk) throws Exception {
        SocketChannel socketChannel = (SocketChannel) sk.channel();
        SocketChannel clientSocketChannel = null;
        if (sk.attachment() == null || (sk.attachment().toString().startsWith("server-") && sk.attachment().toString().endsWith("-"))) {
            String substring = sk.attachment().toString().substring(0, sk.attachment().toString().lastIndexOf("-"));
            String[] split = substring.split("-");
            ByteBuffer byteBuffer = ByteBuffer.allocate(53+split[1].length());
            int num = 0;
            try {
                while (num <= limit) {
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
                            if (s.contains("-success")) {
                                s = s.substring(0, s.indexOf("-success"));
                                String uuid = s.replaceAll("connect-", "");
                                System.out.println("通道建立成功");
                                sk.attach("server-" + uuid);
                                clientServerChannelFlag.add(uuid);

                                //创建通道 连接被代理端
                                System.out.println("创建被代理端通道");
                                String port = uuid.substring(0, uuid.indexOf("-"));
                                if (reflections.containsKey(Integer.parseInt(port))) {
                                    Reflection reflection = reflections.get(Integer.parseInt(port));
                                    SocketChannel beRepresentedSocketChannel = SocketChannel.open(new InetSocketAddress(reflection.getBeRepresentedIp(), reflection.getBeRepresentedPort()));
                                    beRepresentedSocketChannel.configureBlocking(false);
                                    beRepresentedSocketChannel.register(readSelector, SelectionKey.OP_READ, "beRepresented-" + uuid);
                                    beRepresentedSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, "beRepresented-" + uuid);
                                    System.out.println("被代理端通道创建成功");
                                    break;
                                }
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
                return;
            }
            Iterator<SelectionKey> clientIterator = writeSelector.selectedKeys().iterator();
            if (!clientIterator.hasNext()) {
                return;
            }
            while (clientIterator.hasNext()) {
                SelectionKey clientSelectionKey = clientIterator.next();
                if (clientSelectionKey.attachment().toString().equals(skAttachment.replaceAll("server-", "beRepresented-"))) {
                    clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                }
                clientIterator.remove();
            }

            if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
                String s = skAttachment.replaceAll("server-", "");
                String port = s.substring(0, s.indexOf("-"));
                if (reflections.containsKey(Integer.parseInt(port))) {
                    Reflection reflection = reflections.get(Integer.parseInt(port));
                    clientSocketChannel = SocketChannel.open(new InetSocketAddress(reflection.getBeRepresentedIp(), reflection.getBeRepresentedPort()));
                    clientSocketChannel.configureBlocking(false);
                    clientSocketChannel.register(writeSelector, SelectionKey.OP_WRITE, skAttachment.replaceAll("server-", "beRepresented-"));
                    clientSocketChannel.register(readSelector, SelectionKey.OP_READ, skAttachment.replaceAll("server-", "beRepresented-"));
                }
            }

            System.out.println("通道：" + skAttachment);
            if (clientSocketChannel == null || !clientSocketChannel.isConnected()) {
                System.out.println("被代理服务通道已关闭");
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
                        //server通道关闭代表整个流程结束
                        System.out.println(writeSelector.selectNow());
                        Iterator<SelectionKey> tmpClientIterator = writeSelector.selectedKeys().iterator();
                        String uuid = skAttachment.replaceAll("server-", "");
                        clientServerChannelFlag.remove(uuid);
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
                String uuid = skAttachment.replaceAll("server-", "");
                clientServerChannelFlag.remove(uuid);
                while (tmpClientIterator.hasNext()) {
                    SelectionKey next = tmpClientIterator.next();
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
    }

    @Override
    public void write(SelectionKey sk) throws Exception {

    }
}

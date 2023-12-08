package com.zky.client;



import com.zky.server.BaseServerSocketChannelHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy {

    public static Integer limit = 2;
    public static String clientHost = "120.46.189.242";
//        public static String clientHost = "localhost";
    public static Integer clientPort = 3306;
//        public static Integer clientPort = 8080;
    public static Map<String, BaseClientSocketChannelHandler> handlerMap = new ConcurrentHashMap<>();
    static {
        handlerMap.put("server",new ServerSocketChannelHandler());
        handlerMap.put("beRepresented",new BeRepresentedSocketChannelHandler());
        handlerMap.put("cmd",new CMDSocketChannelHandler());
    }

    public static void main(String[] args) throws Exception {
        new ClientProxy().run();
    }
    public void run() throws Exception {
        Selector cmdCreateSelector = Selector.open();
        Selector readSelector = Selector.open();
        Selector writeSelector = Selector.open();

        //连接cmd
        SocketChannel cmdSocketChannel = SocketChannel.open(new InetSocketAddress("localhost", 9099));
        cmdSocketChannel.configureBlocking(false);
        cmdSocketChannel.register(cmdCreateSelector,SelectionKey.OP_WRITE,"cmd-");

        //cmdSelector
        new Thread(()->{
            try {
                while (true) {
                    int select = cmdCreateSelector.select();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = cmdCreateSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey sk = iterator.next();
                        if (sk.isReadable()) {
                            handlerMap.get("cmd").read(sk,readSelector,writeSelector);
                        }else if(sk.isWritable()){
                            handlerMap.get("cmd").write(sk);
                        }
                        iterator.remove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


        /**
         * readSelector
         */
        new Thread(() -> {
            try {
                while (true) {
                    int select = readSelector.selectNow();
                    if (select == 0) {
                        Thread.sleep(100);
                        continue;
                    }
                    while (true) {
                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        if (selectionKeys.size() == 0) {
                            break;
                        }
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {
                            boolean flag = false;
                            SelectionKey sk = iterator.next();
                            if (sk.isReadable()) {
                                BaseClientSocketChannelHandler socketChannelHandler = handlerMap.get(sk.attachment().toString().substring(0, sk.attachment().toString().indexOf("-")));
                                flag = socketChannelHandler.read(sk,readSelector,writeSelector);
                            }
                            if (flag) {
                                iterator.remove();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

}

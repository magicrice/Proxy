package com.zky.context;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSelectorContext {
   public static Selector acceptSelector = null;
   public static Selector readSelector = null;
   public static Selector writeSelector = null;
   public static Map<String, ServerSocketChannel> serverSocketChannelMap = new ConcurrentHashMap<>();

    public ServerSelectorContext() {
    }
    public void start(){
        try {
            acceptSelector = Selector.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServerSocketChannel(String port){
        ServerSocketChannel serverSocketChannel = serverSocketChannelMap.get(port);
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void addServerSocketChannel(String port,ServerSocketChannel serverSocketChannel){
        serverSocketChannelMap.put(port,serverSocketChannel);
    }
    public void rmServerSocketChannel(String port){
        serverSocketChannelMap.remove(port);
    }
}

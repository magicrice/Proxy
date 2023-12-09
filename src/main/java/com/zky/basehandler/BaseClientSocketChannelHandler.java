package com.zky.basehandler;

import com.zky.client.Reflection;
import com.zky.context.ClientSelectorContext;

import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class BaseClientSocketChannelHandler extends ClientSelectorContext {

    public Integer limit = 2;
    public static Set<String> clientServerChannelFlag = new CopyOnWriteArraySet<>();

    public Map<Integer, Reflection> reflections = new ConcurrentHashMap<>();

    public BaseClientSocketChannelHandler() {
    }

    public BaseClientSocketChannelHandler(List<String> list) {
        for (String s : list) {
            String[] split = s.split("->");
            String[] split1 = split[0].split(":");
            reflections.put(Integer.parseInt(split[1]),new Reflection(split1[0],Integer.parseInt(split1[1]),Integer.parseInt(split[1])));
        }
    }

    public abstract void accept() throws Exception;

    public abstract void read(SelectionKey sk)throws Exception;

    public abstract void write(SelectionKey sk) throws Exception;
}

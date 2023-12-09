package com.zky.context;

import com.zky.client.Reflection;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientSelectorContext {
    public static Selector cmdCreateSelector = null;
    public static Selector readSelector = null;
    public static Selector writeSelector = null;
    public static Set<String> clientServerChannelFlag = new CopyOnWriteArraySet<>();
    public static Integer limit = 2;
    public static Map<Integer, Reflection> reflections = new ConcurrentHashMap<>();

    public ClientSelectorContext() {

    }
    public ClientSelectorContext(List<String> list){
        for (String s : list) {
            String[] split = s.split("->");
            String[] split1 = split[0].split(":");
            reflections.put(Integer.parseInt(split[1]),new Reflection(split1[0],Integer.parseInt(split1[1]),Integer.parseInt(split[1])));
        }
    }

    public void start(List<String> list){
        try {
            cmdCreateSelector = Selector.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
            for (String s : list) {
                String[] split = s.split("->");
                String[] split1 = split[0].split(":");
                reflections.put(Integer.parseInt(split[1]),new Reflection(split1[0],Integer.parseInt(split1[1]),Integer.parseInt(split[1])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package com.zky.server;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerChannelHandler {
    //client与server通道
//    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static Map<String,ChannelGroup> channelGroup = new ConcurrentHashMap<>();

    //web访问通道 key为id value为通道
    public static Map<String,ChannelGroup> channelGroupMap = new ConcurrentHashMap<>();

}

package com.zky.client;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChannelHandler {
    //client与server通道
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //被代理端访问通道 key为id value为通道
    public static Map<String,ChannelGroup> channelGroupMap = new ConcurrentHashMap<>();

}

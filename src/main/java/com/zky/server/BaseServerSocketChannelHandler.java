package com.zky.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public abstract class BaseServerSocketChannelHandler {

    public abstract void accept(SelectionKey sk, Selector writeSelector, Selector readSelector) throws Exception;

    public abstract boolean read(SelectionKey sk,Selector writeSelector,Selector acceptSelector)throws Exception;

    public abstract void write();
}

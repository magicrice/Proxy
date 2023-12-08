package com.zky.client;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class BaseClientSocketChannelHandler {

    public abstract void accept() throws Exception;

    public abstract boolean read(SelectionKey sk, Selector readSelector,Selector writeSelector)throws Exception;

    public abstract void write(SelectionKey sk) throws Exception;
}

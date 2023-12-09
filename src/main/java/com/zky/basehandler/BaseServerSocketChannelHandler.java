package com.zky.basehandler;

import com.zky.context.ServerSelectorContext;

import java.nio.channels.SelectionKey;

public abstract class BaseServerSocketChannelHandler extends ServerSelectorContext {

    public abstract void accept(SelectionKey sk) throws Exception;

    public abstract void read(SelectionKey sk)throws Exception;

    public abstract void write();
}

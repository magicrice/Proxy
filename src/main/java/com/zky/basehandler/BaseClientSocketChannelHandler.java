package com.zky.basehandler;

import com.zky.context.ClientSelectorContext;

import java.nio.channels.SelectionKey;
import java.util.List;

public abstract class BaseClientSocketChannelHandler extends ClientSelectorContext {


    public abstract void accept() throws Exception;

    public abstract void read(SelectionKey sk)throws Exception;

    public abstract void write(SelectionKey sk) throws Exception;
}

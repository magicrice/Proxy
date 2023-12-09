package com.zky.context;

import java.io.IOException;
import java.nio.channels.Selector;

public class ClientSelectorContext {
    public static Selector cmdCreateSelector = null;
    public static Selector readSelector = null;
    public static Selector writeSelector = null;

    public ClientSelectorContext() {

    }
    public void start(){
        try {
            cmdCreateSelector = Selector.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

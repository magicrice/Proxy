package com.zky.utils;

import com.zky.client.MsgInfo;

public class MsgUtil {

    public static MsgInfo buildMsg(String uuid,int size, byte[] msgContent) {
        return new MsgInfo(uuid,size,msgContent);
    }

}
 
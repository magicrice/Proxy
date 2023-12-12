package com.zky.utils;

public class MsgUtil {

    public static MsgInfo buildMsg(int type,String uuid,String port,int size, byte[] msgContent) {
        return new MsgInfo(type,uuid,port,size,msgContent);
    }

}
 
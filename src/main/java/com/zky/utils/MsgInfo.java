package com.zky.utils;

public class MsgInfo {

    private int type; //消息类型 1、创建对外服务 2、创建通道 3、普通消息
    private String uuid;
    private String port;
    private int size;
    private byte[] msgContent;

    public MsgInfo() {
    }

    public MsgInfo(int type,String uuid,String port, int size, byte[] msgContent) {
        this.type = type;
        this.uuid = uuid;
        this.port = port;
        this.size = size;
        this.msgContent = msgContent;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(byte[] msgContent) {
        this.msgContent = msgContent;
    }
}
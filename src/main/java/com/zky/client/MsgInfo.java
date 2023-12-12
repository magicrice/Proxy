package com.zky.client;

public class MsgInfo {

    private String uuid;
    private int size;
    private byte[] msgContent;

    public MsgInfo() {
    }

    public MsgInfo(String uuid, int size, byte[] msgContent) {
        this.uuid = uuid;
        this.size = size;
        this.msgContent = msgContent;
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
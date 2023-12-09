package com.zky.client;

public class Reflection {
    private String beRepresentedIp;
    private Integer beRepresentedPort;
    private Integer outPort;
    private Boolean flag;

    public Reflection(String beRepresentedIp, Integer beRepresentedPort, Integer outPort) {
        this.beRepresentedIp = beRepresentedIp;
        this.beRepresentedPort = beRepresentedPort;
        this.outPort = outPort;
        this.flag = false;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }

    public String getBeRepresentedIp() {
        return beRepresentedIp;
    }

    public void setBeRepresentedIp(String beRepresentedIp) {
        this.beRepresentedIp = beRepresentedIp;
    }

    public Integer getBeRepresentedPort() {
        return beRepresentedPort;
    }

    public void setBeRepresentedPort(Integer beRepresentedPort) {
        this.beRepresentedPort = beRepresentedPort;
    }

    public Integer getOutPort() {
        return outPort;
    }

    public void setOutPort(Integer outPort) {
        this.outPort = outPort;
    }
}

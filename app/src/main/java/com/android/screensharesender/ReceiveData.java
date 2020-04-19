package com.android.screensharesender;

public class ReceiveData {
    private int cmd;
    private int size;
    private byte[] data;

    public ReceiveData(int cmd, int size, byte[] data) {
        this.cmd = cmd;
        this.size = size;
        this.data = data;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

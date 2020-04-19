package com.android.screensharesender.connection;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class TcpConnection {
    private static final String TAG = "TcpConnection";
    private Socket socket = null;
    private boolean canReceive = false;

    private OutputStream outputStream;
    private InputStream inputStream;

    private TcpConnectListener tcpConnectListener;
    private TcpDisconnectListener tcpDisconnectListener;

    private TcpConnection() {

    }

    private static class SingletonHolder {
        private static TcpConnection instance = new TcpConnection();
    }

    public static TcpConnection getInstance() {
        return SingletonHolder.instance;
    }

    public interface TcpConnectListener {
        //socket连接成功
        void onSocketConnectSuccess();

        //socket连接失败
        void onSocketConnectFail(String message);

        //tcp连接成功
        void onTcpConnectSuccess();

        //tcp连接失败
        void onTcpConnectFail(String message);

        //socket断开连接
        void onSocketDisconnect(String message);

    }

    public interface TcpDisconnectListener {
        void onTcpDisconnectSuccess();
        void onTcpDisconnectFail(String message);
    }

    public void setTcpConnectListener(TcpConnectListener tcpConnectListener) {
        this.tcpConnectListener = tcpConnectListener;
    }

    public void setTcpDisconnectListener(TcpDisconnectListener tcpDisconnectListener) {
        this.tcpDisconnectListener = tcpDisconnectListener;
    }

    public void connect(String ip, int port, TcpConnectListener tcpConnectListener) {
        try {
            if (socket != null) {
                return;
            }
            socket = new Socket(ip, port);

            // read阻塞时间（超过时间抛出异常）
            socket.setSoTimeout(60 * 1000);

            if (tcpConnectListener != null) {
                // Socket连接成功
                tcpConnectListener.onSocketConnectSuccess();
            }
        } catch (IOException e) {
            if (tcpConnectListener != null) {
                // Socket连接错误
                tcpConnectListener.onSocketConnectFail(e.getMessage());
            }
            e.printStackTrace();
        }

        try {
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            if (tcpConnectListener != null) {
                // TCP连接成功
                tcpConnectListener.onTcpConnectSuccess();
            }
        } catch (IOException e) {
            if (tcpConnectListener != null) {
                // TCP连接失败
                tcpConnectListener.onTcpConnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void disconnect(TcpDisconnectListener tcpDisconnectListener) {
        try {
            if (socket == null) {
                return;
            }
            socket.close();
            socket = null;
            if (tcpDisconnectListener != null) {
                tcpDisconnectListener.onTcpDisconnectSuccess();
            }
        } catch (IOException e) {
            if (tcpDisconnectListener != null) {
                tcpDisconnectListener.onTcpDisconnectFail(e.getMessage());
            }
            e.printStackTrace();
        }
    }

//    // TODO
//    public void startReceiving() {
//        canReceive = true;
//        byte[] bytes = new byte[1024];
//        while (canReceive) {
//            try {
//                int length = inputStream.read(bytes);
//            } catch (IOException e) {
//                // 与接收端连接断开
//                canReceive = false;
//                if (tcpConnectListener != null) {
//                    tcpConnectListener.onSocketDisconnect(e.getMessage());
//                }
//                e.printStackTrace();
//            }
//        }
//    }

    public void sendData(byte[] data) {
        try {
            if (outputStream != null) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            // 与接收器连接断开
            if (tcpConnectListener != null) {
                tcpConnectListener.onSocketDisconnect(e.getMessage());
            }
            e.printStackTrace();
        }
    }

//    public void release() {
//        try {
//            if (socket != null) {
//                socket.close();
//                socket = null;
//            }
//            if (outputStream != null) {
//                outputStream.close();
//                outputStream = null;
//            }
//            if (inputStream != null) {
//                inputStream.close();
//                inputStream = null;
//            }
//            canReceive = false;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

}

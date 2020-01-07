package com.android.screensharesender;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.android.screensharesender.common.base.BasePresenter;
import com.android.screensharesender.model.SenderManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SenderPresenter extends BasePresenter<SenderContract.IView> implements SenderContract.IPresenter {

    private static final int MESSAGE_UPDATE_TEXT_VIEW = 0;
    private static final int MESSAGE_UPDATE_UI = 1;
    private static final int PORT = 9988;
    private String HOST = "172.18.141.183";
    private Socket socket;
    private String data;
    private PrintWriter printWriter;
    private BufferedReader bufferedReader;
    private ExecutorService executorService = null;
    private boolean canReceive = false;

    public SenderPresenter() {
        initThreadPool();
    }

    @SuppressLint("HandlerLeak")
    private Handler updateUIHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MESSAGE_UPDATE_TEXT_VIEW) {
                view.updateTextView((String)msg.obj);
            } else {
                view.updateUI();
            }
        }
    };

    @Override
    public String getData() {
        return SenderManager.Companion.getInstance().getData();
    }

    @Override
    public void onConnect() {
        startConnecting();
    }

    @Override
    public void onDisconnect() {
        startSending("客户端断开连接");
    }

    @Override
    public void onSendMessage(final String message) {
        startSending(message);
    }

    private void initThreadPool() {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                        KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

    public void startConnecting() {
        //执行任务
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(HOST, PORT);
                    socket.setSoTimeout(60 * 1000);
                    data = getData(); // 获取数据
                    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream(), "UTF-8")), true);
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    canReceive = true;
                    startReceiving();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void startReceiving() {
        executorService.execute(new Runnable() {
            String message = null;
            Message handlerMessage;

            @Override
            public void run() {
                while (canReceive) {
                    try {
                        message = bufferedReader.readLine();
                        if (message != null) {
                            handlerMessage = updateUIHandler.obtainMessage();
                            handlerMessage.what = MESSAGE_UPDATE_TEXT_VIEW;
                            handlerMessage.obj = message;
                            handlerMessage.sendToTarget();
                            message = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void startSending(final String message) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (printWriter != null) {
                    printWriter.println(message);
                }
                if (message.equals("客户端断开连接")) {
                    release();
                }
            }
        });
    }

    private void release() {
        try {
            socket.close();
            printWriter.close();
            bufferedReader.close();
            canReceive = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

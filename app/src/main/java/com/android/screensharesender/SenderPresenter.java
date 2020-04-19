package com.android.screensharesender;

import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;

import com.android.screensharesender.common.base.BasePresenter;
import com.android.screensharesender.connection.TcpConnection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogRecord;

public class SenderPresenter extends BasePresenter<SenderContract.IView> implements SenderContract.IPresenter {

    private static final String TAG = "SenderPresenter";

    private ExecutorService executorService = null;
    private ScreenSender screenSender;
    private TcpConnection tcpConnection = TcpConnection.getInstance();

    private Handler updateUiHandler = new Handler();

    public SenderPresenter() {
        initThreadPool();
    }

    @Override
    public void connect() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.connect("192.168.100.5", 9988, new TcpConnection.TcpConnectListener() {
                    @Override
                    public void onSocketConnectSuccess() {

                    }

                    @Override
                    public void onSocketConnectFail(String message) {

                    }

                    @Override
                    public void onTcpConnectSuccess() {
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onConnectSuccess();
                            }
                        });

                    }

                    @Override
                    public void onTcpConnectFail(String message) {

                    }

                    @Override
                    public void onSocketDisconnect(String message) {
                        // 与接收器断开连接
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onDisconnectSuccess();
                            }
                        });
                    }
                });
                // tcpConnection.startReceiving();
            }
        });
    }

    @Override
    public void disconnect() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                tcpConnection.disconnect(new TcpConnection.TcpDisconnectListener() {
                    @Override
                    public void onTcpDisconnectSuccess() {
                        updateUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.onDisconnectSuccess();
                            }
                        });
                    }

                    @Override
                    public void onTcpDisconnectFail(String message) {

                    }
                });
            }
        });

    }

    @Override
    public void startScreenShare(MediaProjection mediaProjection) {
        screenSender = new ScreenSender(mediaProjection);
        executorService.execute(screenSender);
    }

    @Override
    public void stopScreenShare() {
        if (screenSender != null) {
            screenSender.stop();
            screenSender = null;
        }
    }

    private void initThreadPool() {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                        KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

}

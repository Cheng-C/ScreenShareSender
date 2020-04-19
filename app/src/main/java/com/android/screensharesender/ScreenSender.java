package com.android.screensharesender;


import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.android.screensharesender.connection.TcpConnection;
import com.android.screensharesender.utils.ByteUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 获取屏幕内容，进行编码后发送至传屏接收端
 */
public class ScreenSender implements Runnable {

    private static final String TAG = "ScreenEncoder";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Surface surface;

    private ExecutorService executorService = null;
    private MediaCodec encoder;
    private int width;
    private int height;
    private int screenDensityDpi;
    private String mimeType;
    private int frameRate;
    private int bitRate;
    private int IFrameInterval;

    private ByteBuffer outputBuffer;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private MediaFormat outputFormat;

    private AtomicBoolean quit = new AtomicBoolean(false);

    private TcpConnection tcpConnection = TcpConnection.getInstance();

    private void initThreadPool() {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

    /**
     * @param mediaProjection 屏幕镜像（一种token，赋予应用程序捕获屏幕内容和/或录制系统音频的能力。）
     * @param width 虚拟显示的宽度（像素）
     * @param height 虚拟显示的高度（像素）
     * @param screenDensityDpi dpi来表示VirtualDisplay的密度
     * @param mimeType mime类型
     * @param frameRate 帧率
     * @param bitRate 码率（每秒传送的比特数）
     * @param IFrameInterval I帧间隔
     */
    public ScreenSender(MediaProjection mediaProjection, int width, int height, int screenDensityDpi,
                        String mimeType, int frameRate, int bitRate, int IFrameInterval) {
        this.mediaProjection = mediaProjection;
        this.width = width;
        this.height = height;
        this.screenDensityDpi = screenDensityDpi;
        this.mimeType = mimeType;
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.IFrameInterval = IFrameInterval;
        initThreadPool();
    }

    public ScreenSender(MediaProjection mediaProjection) {
        this(mediaProjection, 1280, 1920, 1,
                "video/avc", 30, 6000000, 10);
    }

    @Override
    public void run() {
        prepareEncoder();
        virtualDisplay = mediaProjection.createVirtualDisplay(TAG + "-display", width, height, screenDensityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null);
    }

    private void prepareEncoder() {
        // width 内容的宽度(以像素为单位) height 内容的高度(以像素为单位)
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFrameInterval);

        try {
            encoder = MediaCodec.createEncoderByType(mimeType);
            encoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();
            encoder.start();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while(!quit.get()) {
                        Log.i(TAG, "run: 执行1");
                        int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                        Log.i(TAG, "run: 执行2");
                        if (outputIndex >= 0) {
                            outputBuffer = encoder.getOutputBuffer(outputIndex);
                            int size = bufferInfo.size;
                            if (size <= 0) {
                                continue;
                            }

                            byte[] buffer = new byte[size];
                            outputBuffer.get(buffer);

                            Log.i(TAG, "run: size" + size);
                            byte[] data = buildSendContent(Constants.SCREEN_SHARE, size, buffer);
                            tcpConnection.sendData(data);
                            encoder.releaseOutputBuffer(outputIndex, false);
                        }
                    }
                    release();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] buildSendContent(int cmd, int bufferSize, byte[] buffer) {
        ByteBuffer data = ByteBuffer.allocate(8 + bufferSize);
        data.put(ByteUtils.int2Bytes(cmd));
        data.put(ByteUtils.int2Bytes(bufferSize));
        if (bufferSize != 0) {
            data.put(buffer);
        }
        return data.array();
    }

    public void stop() {
        Log.i(TAG, "stop");
        quit.set(true);

        // 发送停止投屏消息
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                byte[] data = buildSendContent(Constants.STOP_SCREEN_SHARE, 0, null);
                tcpConnection.sendData(data);
            }
        });
    }

    public void release() {
        Log.i(TAG, "release");
        if (encoder != null) {
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

    }
}

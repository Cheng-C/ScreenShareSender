/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.screensharesender;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;


public class ScreenRecorder extends Thread {
    private static final String TAG = "ScreenRecorder";

    private int width;
    private int height;
    private int bitrate;
    private int dpi;
    private String dstPath;
    private MediaProjection mediaProjection;
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc"; // H.264 高级视频编码
    private static final int FRAME_RATE = 30; // 30 fps
    private static final int I_FRAME_INTERVAL = 10; // 10 seconds between I-frames
    private static final int TIMEOUT_US = 10000;

    private MediaCodec encoder;
    private Surface surface;
    private MediaMuxer mediaMuxer;
    private boolean mediaMuxerStarted = false;
    private int videoTrackIndex = -1;
    private AtomicBoolean quit = new AtomicBoolean(false);
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private VirtualDisplay virtualDisplay;

    public ScreenRecorder(int width, int height, int bitrate, int dpi, MediaProjection mediaProjection, String dstPath) {
        super(TAG);
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.dpi = dpi;
        this.mediaProjection = mediaProjection;
        this.dstPath = dstPath;
    }

    public ScreenRecorder(MediaProjection mediaProjection) {
        // 480p 2Mbps
        this(640, 480, 2000000, 1, mediaProjection, "/sdcard/test.mp4");
    }

    /**
     * stop task
     */
    public final void quit() {
        quit.set(true);
    }

    @Override
    public void run() {
        try {
            try {
                prepareEncoder();
                mediaMuxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);// mp4

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            virtualDisplay = mediaProjection.createVirtualDisplay(TAG + "-display",
                    width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    surface, null, null);
            Log.d(TAG, "created virtual display: " + virtualDisplay);
            recordVirtualDisplay();

        } finally {
            release();
            Log.d(TAG, "资源释放完成");
        }
    }

    private void prepareEncoder() throws IOException {

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

        Log.d(TAG, "created video format: " + format);
        encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = encoder.createInputSurface();
        Log.d(TAG, "created input surface: " + surface);
        encoder.start();
    }

    private void recordVirtualDisplay() {
        while (!quit.get()) {
            int index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US);
            Log.i(TAG, "dequeue output buffer index=" + index);
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                resetOutputFormat();

            } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "retrieving buffers time out!");
                try {
                    // wait 10ms
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            } else if (index >= 0) {

                if (!mediaMuxerStarted) {
                    throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                }
                encodeToVideoTrack(index);

                encoder.releaseOutputBuffer(index, false);
            }
        }
    }

    private void encodeToVideoTrack(int index) {
        ByteBuffer encodedData = encoder.getOutputBuffer(index);

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            // 当我们得到信息输出格式改变的状态时，编解码器配置数据被取出并馈送到muxer。
            // Ignore it.
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            bufferInfo.size = 0;
        }
        if (bufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            Log.d(TAG, "got buffer, info: size=" + bufferInfo.size
                    + ", presentationTimeUs=" + bufferInfo.presentationTimeUs
                    + ", offset=" + bufferInfo.offset);
        }
        if (encodedData != null) {
            encodedData.position(bufferInfo.offset);
            encodedData.limit(bufferInfo.offset + bufferInfo.size);
            mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
            Log.i(TAG, "sent " + bufferInfo.size + " bytes to muxer...");
        }
    }

    private void resetOutputFormat() {
        // should happen before receiving buffers, and should only happen once
        // 应该在接收缓冲区之前发生，并且应该只发生一次
        if (mediaMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        MediaFormat newFormat = encoder.getOutputFormat();

        Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
        // 添加一个视频轨道，并返回对应的ID
        videoTrackIndex = mediaMuxer.addTrack(newFormat);
        // 添加完所有音视频轨道之后，需要调用这个方法告诉Muxer，我要开始写入数据了。
        // 需要注意的是，调用了这个方法之后，我们是无法再次addTrack了的。
        mediaMuxer.start();
        mediaMuxerStarted = true;
        Log.i(TAG, "started media muxer, videoIndex=" + videoTrackIndex);
    }

    private void release() {
        if (encoder != null) {
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (mediaMuxer != null) {
            Log.d(TAG, "release: 停止写入数据，并生成文件");
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        }
    }
}

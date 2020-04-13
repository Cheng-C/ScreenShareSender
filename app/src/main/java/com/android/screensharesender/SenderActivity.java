package com.android.screensharesender;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.screensharesender.common.base.BaseMvpActivity;

import java.io.File;

public class SenderActivity extends BaseMvpActivity<SenderContract.IPresenter> implements SenderContract.IView {

    private static final String TAG = "SenderActivity";
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private Surface surface;
    private SurfaceView surfaceView;
    private MediaProjectionManager mediaProjectionManager;
    private ScreenRecorder recorder;

    private Button connectButton;
    private Button disconnectButton;
    private EditText messageEditText;
    private Button sendButton;
    private TextView recordTextView;
    private Button recordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        connectButton = findViewById(R.id.btnConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onConnect();
            }
        });

        disconnectButton = findViewById(R.id.btnDisconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onDisconnect();
            }
        });

        messageEditText = findViewById(R.id.etMessage);

        sendButton = findViewById(R.id.btnSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                presenter.onSendMessage(message);
            }
        });

        recordTextView = findViewById(R.id.tvRecord);

        recordButton = findViewById(R.id.btnRecord);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder != null) {
                    recorder.quit();
                    recorder = null;
                    recordButton.setText("录屏");
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            presenter.onDisconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: 执行");
        if(recorder != null){
            recorder.quit();
            recorder = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "mediaProjection为空");
            return;
        }
        // video size
        final int width = 1280;
        final int height = 1920;
        File file = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Screenshots"),
                "record-" + width + "x" + height + "-" + System.currentTimeMillis() + ".mp4");
        final int bitrate = 6000000;
        recorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
        recorder.start();
        recordButton.setText("停止录屏");
        Toast.makeText(this, "正在录屏...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected SenderContract.IPresenter injectPresenter() {
        return new SenderPresenter();
    }

    @Override
    public void updateUI() {

    }

    @Override
    public void updateTextView(String message) {
        recordTextView.append(message + "\n");
    }
}

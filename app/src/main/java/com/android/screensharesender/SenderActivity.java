package com.android.screensharesender;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.screensharesender.common.base.BaseMvpActivity;

public class SenderActivity extends BaseMvpActivity<SenderContract.IPresenter> implements SenderContract.IView {

    private static final String TAG = "SenderActivity";
    private static final int REQUEST_SCREEN_SHARE = 1;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;

    private Button connectButton;
    private Button disconnectButton;
    private Button screenShareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        connectButton = findViewById(R.id.btnConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connectButton.getText().equals("连接")) {
                    presenter.connect();
                } else {
                    presenter.disconnect();
                }
            }
        });

        screenShareButton = findViewById(R.id.btnScreenShare);
        screenShareButton.setVisibility(View.INVISIBLE);
        screenShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mediaProjection != null) {
                   presenter.stopScreenShare();
                   mediaProjection.stop();
                   mediaProjection = null;
                   screenShareButton.setText("传屏");
                } else {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_SCREEN_SHARE);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            // 如果应用退出
            presenter.stopScreenShare();
            presenter.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN_SHARE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "未获取录屏权限", Toast.LENGTH_SHORT).show();
                return;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

            Toast.makeText(this, "正在传屏...", Toast.LENGTH_SHORT).show();

            presenter.startScreenShare(mediaProjection);
            screenShareButton.setText("停止传屏");
        }
    }

    @Override
    protected SenderContract.IPresenter injectPresenter() {
        return new SenderPresenter();
    }

    @Override
    public void onConnectSuccess() {
        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
        screenShareButton.setVisibility(View.VISIBLE);
        connectButton.setText("断开连接");
    }

    @Override
    public void onDisconnectSuccess() {
        Toast.makeText(this, "断开连接成功", Toast.LENGTH_SHORT).show();
        screenShareButton.setVisibility(View.INVISIBLE);
        if (mediaProjection != null) {
            presenter.stopScreenShare();
            mediaProjection.stop();
            mediaProjection = null;
            screenShareButton.setText("传屏");
        }
        connectButton.setText("连接");
    }
}

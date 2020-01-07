package com.android.screensharesender;

import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.screensharesender.common.base.BaseMvpActivity;

public class SenderActivity extends BaseMvpActivity<SenderContract.IPresenter> implements SenderContract.IView {

    private static final String TAG = "SenderActivity";

    private Surface surface;
    private SurfaceView surfaceView;

    private Button connectButton;
    private Button disconnectButton;
    private EditText messageEditText;
    private Button sendButton;
    private TextView recordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        connectButton = (Button) findViewById(R.id.btnConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onConnect();
            }
        });

        disconnectButton = (Button) findViewById(R.id.btnDisconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onDisconnect();
            }
        });

        messageEditText = (EditText) findViewById(R.id.etMessage);

        sendButton = (Button) findViewById(R.id.btnSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageEditText.getText().toString();
                presenter.onSendMessage(message);
            }
        });

        recordTextView = (TextView) findViewById(R.id.tvRecord);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            presenter.onDisconnect();
        }
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

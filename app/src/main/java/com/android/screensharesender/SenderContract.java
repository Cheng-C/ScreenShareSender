package com.android.screensharesender;

import android.media.projection.MediaProjection;

import com.android.screensharesender.common.base.IBasePresenter;
import com.android.screensharesender.common.base.IBaseView;

public class SenderContract {
    public interface IPresenter extends IBasePresenter<SenderContract.IView> {
        void connect();
        void disconnect();
        void startScreenShare(MediaProjection mediaProjection);
        void stopScreenShare();
    }

    public interface IView extends IBaseView {
        void onConnectSuccess();
        void onDisconnectSuccess();
    }
}

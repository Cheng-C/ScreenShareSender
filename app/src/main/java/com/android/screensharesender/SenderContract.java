package com.android.screensharesender;

import com.android.screensharesender.common.base.IBasePresenter;
import com.android.screensharesender.common.base.IBaseView;

public class SenderContract {
    public interface IPresenter extends IBasePresenter<SenderContract.IView> {
        String getData();
        void onConnect();
        void onDisconnect();
        void onSendMessage(String message);
        void onSetUpMediaProjection();
        void onSetUpVirtualDisplay();
    }

    public interface IView extends IBaseView {
        void updateUI();
        void updateTextView(String message);
    }
}

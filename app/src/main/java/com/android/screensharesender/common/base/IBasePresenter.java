package com.android.screensharesender.common.base;

public interface IBasePresenter<V extends IBaseView> {

    void attachView(V view);

    void detachView(V view);

}

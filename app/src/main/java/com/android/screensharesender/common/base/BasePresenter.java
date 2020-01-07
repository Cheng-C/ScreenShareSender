package com.android.screensharesender.common.base;

public abstract class BasePresenter<V extends IBaseView> implements IBasePresenter<V> {

    protected V view;

    @Override
    public void attachView(V view) {
        this.view = view;
    }

    @Override
    public void detachView(V view) {
        this.view = null;
    }

}

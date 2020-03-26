package io.github.trojan_gfw.igniter.common.mvp;

public interface BaseView<T extends BasePresenter> {
    void setPresenter(T presenter);
}

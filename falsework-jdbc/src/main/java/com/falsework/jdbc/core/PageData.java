package com.falsework.jdbc.core;

/**
 * 包含总行数的结果集
 * Created by LiShiwu on 06/12/2017.
 */
public class PageData<T> {
    private long total;
    private T data;

    public PageData() {
    }

    public PageData(int total, T data) {
        this.total = total;
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

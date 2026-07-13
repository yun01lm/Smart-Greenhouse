package com.greenhouse.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 后端统一分页格式
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public List<T> getList() { return list; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}

package com.greenhouse.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应包装
 */
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public PageResult() {}

    public PageResult(List<T> list, long total, int page, int size) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }

    public static <T> PageResultBuilder<T> builder() {
        return new PageResultBuilder<>();
    }

    public static class PageResultBuilder<T> {
        private List<T> list;
        private long total;
        private int page;
        private int size;

        public PageResultBuilder<T> list(List<T> list) { this.list = list; return this; }
        public PageResultBuilder<T> total(long total) { this.total = total; return this; }
        public PageResultBuilder<T> page(int page) { this.page = page; return this; }
        public PageResultBuilder<T> size(int size) { this.size = size; return this; }
        public PageResult<T> build() { return new PageResult<>(list, total, page, size); }
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

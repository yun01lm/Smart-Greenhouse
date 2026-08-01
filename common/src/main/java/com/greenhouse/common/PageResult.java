package com.greenhouse.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应格式
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 数据列表 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码（从1开始） */
    private int page;

    /** 每页条数 */
    private int size;

    public static <T> PageResult<T> of(List<T> list, long total, int page, int size) {
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 从 Spring Data Page 对象构建
     */
    public static <T> PageResult<T> of(org.springframework.data.domain.Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,  // Spring Data page 从 0 开始
                page.getSize()
        );
    }

    // ===== Builder 模式（用于灵活构建分页响应）=====

    public static <T> PageResultBuilder<T> builder() {
        return new PageResultBuilder<>();
    }

    public static class PageResultBuilder<T> {
        private List<T> content;
        private long total;
        private int page;
        private int size;

        public PageResultBuilder<T> list(List<T> content) { this.content = content; return this; }
        public PageResultBuilder<T> total(long total) { this.total = total; return this; }
        public PageResultBuilder<T> page(int page) { this.page = page; return this; }
        public PageResultBuilder<T> size(int size) { this.size = size; return this; }
        public PageResult<T> build() { return new PageResult<>(content, total, page, size); }
    }
}
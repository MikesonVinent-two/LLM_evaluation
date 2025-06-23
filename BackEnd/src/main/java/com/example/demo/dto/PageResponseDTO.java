package com.example.demo.dto;

import java.util.List;

/**
 * 通用分页响应DTO
 * 
 * @param <T> 数据项类型
 */
public class PageResponseDTO<T> {

    /**
     * 当前页的数据项列表
     */
    private List<T> content;

    /**
     * 总记录数
     */
    private long totalElements;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 当前页码（从0开始）
     */
    private int pageNumber;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 是否为首页
     */
    private boolean first;

    /**
     * 是否为末页
     */
    private boolean last;

    /**
     * 构造函数
     * 
     * @param content 数据列表
     * @param totalElements 总记录数
     * @param pageNumber 当前页码
     * @param pageSize 每页大小
     */
    public PageResponseDTO(List<T> content, long totalElements, int pageNumber, int pageSize) {
        this.content = content;
        this.totalElements = totalElements;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        this.first = pageNumber == 0;
        this.last = pageNumber >= totalPages - 1;
    }
    
    /**
     * 获取内容列表
     * @return 内容列表
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 设置内容列表
     * @param content 内容列表
     */
    public void setContent(List<T> content) {
        this.content = content;
    }

    /**
     * 获取总记录数
     * @return 总记录数
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 设置总记录数
     * @param totalElements 总记录数
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * 获取总页数
     * @return 总页数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 设置总页数
     * @param totalPages 总页数
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 获取当前页码
     * @return 当前页码
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * 设置当前页码
     * @param pageNumber 当前页码
     */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /**
     * 获取每页记录数
     * @return 每页记录数
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页记录数
     * @param pageSize 每页记录数
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 是否为首页
     * @return 是否为首页
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 设置是否为首页
     * @param first 是否为首页
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * 是否为末页
     * @return 是否为末页
     */
    public boolean isLast() {
        return last;
    }

    /**
     * 设置是否为末页
     * @param last 是否为末页
     */
    public void setLast(boolean last) {
        this.last = last;
    }
} 
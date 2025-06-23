package com.example.demo.dto;

/**
 * 选项数据传输对象，用于表示选择题的选项
 */
public class Option {
    private String id;
    private String text;

    // 默认构造函数
    public Option() {
    }

    // 带参数的构造函数
    public Option(String id, String text) {
        this.id = id;
        this.text = text;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Option{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
} 
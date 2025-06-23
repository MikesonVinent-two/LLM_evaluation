package com.example.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文本预处理工具类，用于处理模型回答中的特殊标记和格式
 */
public class TextPreprocessor {
    
    private static final Logger logger = LoggerFactory.getLogger(TextPreprocessor.class);
    
    // 匹配思考过程的正则表达式
    private static final Pattern THINK_PATTERN = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
    private static final Pattern THINKING_PATTERN = Pattern.compile("<thinking>(.*?)</thinking>", Pattern.DOTALL);
    private static final Pattern REASONING_PATTERN = Pattern.compile("<reasoning>(.*?)</reasoning>", Pattern.DOTALL);
    private static final Pattern WORK_PATTERN = Pattern.compile("<work>(.*?)</work>", Pattern.DOTALL);
    private static final Pattern THOUGHT_PATTERN = Pattern.compile("<thought>(.*?)</thought>", Pattern.DOTALL);
    
    /**
     * 移除模型回答中的思考过程标记
     * 
     * @param text 原始文本
     * @return 处理后的文本
     */
    public static String removeThinkingProcess(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 记录原始文本长度
        int originalLength = text.length();
        
        // 移除所有思考过程标记
        String result = text;
        result = THINK_PATTERN.matcher(result).replaceAll("");
        result = THINKING_PATTERN.matcher(result).replaceAll("");
        result = REASONING_PATTERN.matcher(result).replaceAll("");
        result = WORK_PATTERN.matcher(result).replaceAll("");
        result = THOUGHT_PATTERN.matcher(result).replaceAll("");
        
        // 如果文本有变化，记录日志
        if (result.length() != originalLength) {
            logger.debug("已移除思考过程标记，原始长度: {}，处理后长度: {}", originalLength, result.length());
        }
        
        // 移除多余的空行和首尾空白
        result = result.replaceAll("(?m)^[ \t]*\r?\n", "").trim();
        
        return result;
    }
    
    /**
     * 提取模型回答中的思考过程
     * 
     * @param text 原始文本
     * @return 提取的思考过程，如果没有则返回空字符串
     */
    public static String extractThinkingProcess(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 尝试匹配各种思考过程标记
        Matcher thinkMatcher = THINK_PATTERN.matcher(text);
        if (thinkMatcher.find()) {
            return thinkMatcher.group(1).trim();
        }
        
        Matcher thinkingMatcher = THINKING_PATTERN.matcher(text);
        if (thinkingMatcher.find()) {
            return thinkingMatcher.group(1).trim();
        }
        
        Matcher reasoningMatcher = REASONING_PATTERN.matcher(text);
        if (reasoningMatcher.find()) {
            return reasoningMatcher.group(1).trim();
        }
        
        Matcher workMatcher = WORK_PATTERN.matcher(text);
        if (workMatcher.find()) {
            return workMatcher.group(1).trim();
        }
        
        Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(text);
        if (thoughtMatcher.find()) {
            return thoughtMatcher.group(1).trim();
        }
        
        return "";
    }
    
    /**
     * 清理并标准化文本，移除多余空白、控制字符等
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 1. 移除思考过程标记
        String result = removeThinkingProcess(text);
        
        // 2. 移除控制字符（ASCII 0-31，除了制表符、换行符和回车符）
        result = result.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        
        // 3. 标准化空白字符（多个空格变成一个）
        result = result.replaceAll("\\s+", " ");
        
        // 4. 移除首尾空白
        result = result.trim();
        
        return result;
    }
} 